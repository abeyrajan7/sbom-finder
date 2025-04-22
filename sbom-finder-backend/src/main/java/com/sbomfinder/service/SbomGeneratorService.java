package com.sbomfinder.service;

import com.sbomfinder.model.Device;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.SbomRepository;
import com.sbomfinder.repository.SoftwarePackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.sbomfinder.util.GitHubReleaseFetcher;
import java.util.Optional;
import java.util.UUID;



@Service
public class SbomGeneratorService {

    @Autowired
    private SbomRepository sbomRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;

    @Autowired
    private SbomService sbomService;

    public SbomGenerationResult generateSbomAndDeviceFromDirectory(
            Path extractedDir,
            String deviceName,
            String category,
            String manufacturer,
            String operatingSystem,
            String osVersion,
            String kernelVersion,
            String sourceType
    ) throws IOException {

        // 1. Search for dependency files
        List<Path> dependencyFiles = Files.walk(extractedDir)
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return name.equals("package.json") ||
                            name.equals("pom.xml") ||
                            name.equals("build.gradle") ||
                            name.contains("requirement") ||
                            name.equals("pipfile") ||
                            name.equals("setup.py") ||
                            name.equals("go.mod") ||
                            name.equals("composer.json") ||
                            name.equals("cargo.toml");
                })
                .collect(Collectors.toList());

        if (dependencyFiles.isEmpty()) {
            throw new IllegalArgumentException("No supported dependency file found in the extracted source.");
        }

        // 2. Extract version from the first matching dependency file
        String version = GitHubReleaseFetcher.extractVersionFromDependencyFile(dependencyFiles.get(0));

        if (version == null || version.isBlank()) {
            version = "Unknown Release"; // Fallback if not found
        }

        // 3. Check if Device exists
        Optional<Device> existingDevice = deviceRepository.findByDeviceNameAndManufacturerAndCategory(deviceName, manufacturer, category);
        Device device;
        if (existingDevice.isPresent()) {
            device = existingDevice.get();
            // 4. Now check if SBOM for same device and version already exists
            Optional<Sbom> existingSbom = sbomRepository.findByDeviceAndVersion(device, version);
            if (existingSbom.isPresent()) {
                throw new IllegalStateException("An SBOM already exists for this device and version: " + version);
            }
        } else {
            // Device doesn't exist, create new one
            device = new Device(deviceName, manufacturer, category, operatingSystem, osVersion, kernelVersion, "N/A");
            device = deviceRepository.save(device);
        }

        // 5. Now create SBOM entry
        String hash = UUID.randomUUID().toString(); // Use a random hash or compute a proper hash if needed
        Sbom sbom = new Sbom(
                "Manual Upload",
                "N/A",
                "N/A",
                "N/A",
                LocalDateTime.now(),
                "Source Upload",
                "CustomTool",
                hash
        );
        sbom.setDevice(device);
        sbom.setVersion(version);
        sbom = sbomRepository.save(sbom);

        device.setSbom(sbom);
        deviceRepository.save(device);

        // 6. Extract and save packages
        List<SoftwarePackage> allPackages = new ArrayList<>();
        for (Path depFile : dependencyFiles) {
            List<SoftwarePackage> extractedPackages = sbomService.extractPackagesFromDependencyFile(depFile, sbom, device);
            allPackages.addAll(extractedPackages);
        }

        for (SoftwarePackage pkg : allPackages) {
            softwarePackageRepository.save(pkg);
            sbomService.checkAndSaveVulnerabilities(pkg);
        }

        return new SbomGenerationResult(version, device);
    }

}

package com.sbomfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.SbomRepository;
import com.sbomfinder.repository.SoftwarePackageRepository;
import com.sbomfinder.util.GitHubReleaseFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sbomfinder.model.SbomArchive;
import com.sbomfinder.repository.SbomArchiveRepository;
import com.sbomfinder.repository.ExternalReferenceRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

    @Autowired
    private SbomArchiveRepository sbomArchiveRepository;

    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    @Autowired
    private SbomArchiveService sbomArchiveService;

    public SbomGenerationResult generateSbomAndDeviceFromDirectory(
            Path extractedDir,
            String deviceName,
            String category,
            String manufacturer,
            String operatingSystem,
            String osVersion,
            String kernelVersion,
            String sourceType
    ) throws IOException, NoSuchAlgorithmException {

        // 1. Find supported dependency files
        List<Path> dependencyFiles = Files.walk(extractedDir)
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return name.equals("package.json") ||
                            name.equals("package-lock.json") ||
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

        // 2. Combine and normalize content from all files to generate consistent hash
        dependencyFiles.sort(Comparator.comparing(Path::toString)); // consistent order
        StringBuilder combinedContent = new StringBuilder();
        for (Path path : dependencyFiles) {
            String content = Files.readString(path).replaceAll("\\s+", "");
            combinedContent.append(content);
        }
        String hash = computeSHA256(combinedContent.toString());

        // 3. Check if this exact SBOM hash already exists
        Optional<Sbom> existingByHash = sbomRepository.findByHash(hash);
        if (existingByHash.isPresent()) {
            throw new IllegalStateException("Duplicate SBOM source. This dependency content was already uploaded.");
        }

        // 4. Extract version info (best effort)
        String version = GitHubReleaseFetcher.extractVersionFromDependencyFile(dependencyFiles.get(0));
        if (version == null || version.isBlank()) {
            version = "Unknown Release";
        }

        // 5. Get or create Device
        Optional<Device> existingDevice = deviceRepository.findByDeviceNameAndManufacturerAndCategory(deviceName, manufacturer, category);
        Device device;
        if (existingDevice.isPresent()) {
            device = existingDevice.get();

            // Check if this version already exists for the same device
            Optional<Sbom> existingVersioned = sbomRepository.findByDeviceAndVersion(device, version);
            if (existingVersioned.isPresent()) {
                throw new IllegalStateException("An SBOM already exists for this device and version: " + version);
            }
        } else {
            device = new Device(deviceName, manufacturer, category, operatingSystem, osVersion, kernelVersion, "N/A");
            device = deviceRepository.save(device);
        }

        // 6. Save SBOM entry
        Sbom sbom = new Sbom(
                "Manual Upload",
                "N/A",
                "N/A",
                "N/A",
                LocalDateTime.now(),
                sourceType,
                "CustomTool",
                hash
        );
        sbom.setDevice(device);
        sbom.setVersion(version);
        sbom = sbomRepository.save(sbom);

        device.setSbom(sbom);
        deviceRepository.save(device);

        // 7. Extract packages from all dependency files
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



    //From Dependency File
    public SbomGenerationResult generateSbomAndDeviceFromDependencyFile(
            Path dependencyFilePath,
            String deviceName,
            String category,
            String manufacturer,
            String operatingSystem,
            String osVersion,
            String kernelVersion
    ) throws IOException, NoSuchAlgorithmException {
        String hash = computeNormalizedSHA256(dependencyFilePath);
        Optional<Sbom> existingByHash = sbomRepository.findByHash(hash);
        if (existingByHash.isPresent()) {
            throw new IllegalStateException("Duplicate SBOM source. This dependency file was already uploaded.");
        }
        // First, check if device exists or create new
        Optional<Device> existingDevice = deviceRepository.findByDeviceNameAndManufacturerAndCategory(deviceName, manufacturer, category);
        Device device;
        if (existingDevice.isPresent()) {
            device = existingDevice.get();
        } else {
            device = new Device(deviceName, manufacturer, category, operatingSystem, osVersion, kernelVersion, "N/A");
            device = deviceRepository.save(device);
        }

        // Create new SBOM record
        Sbom sbom = new Sbom(
                "Manual Upload",
                "N/A",
                "N/A",
                "N/A",
                LocalDateTime.now(),
                "Dependency Upload",
                "CustomTool",
                hash
        );
        sbom.setDevice(device);
        sbom.setVersion("Unknown Release"); // No specific version info for dependency-only uploads
        sbomRepository.save(sbom);

        device.setSbom(sbom);
        deviceRepository.save(device);

        // Extract packages
        List<SoftwarePackage> packages = sbomService.extractPackagesFromDependencyFile(dependencyFilePath, sbom, device);

        for (SoftwarePackage pkg : packages) {
            softwarePackageRepository.save(pkg);
            sbomService.checkAndSaveVulnerabilities(pkg);
        }

        return new SbomGenerationResult("Unknown Release", device);
    }

    private String computeNormalizedSHA256(Path file) throws IOException, NoSuchAlgorithmException {
            // Normalize content
            List<String> lines = Files.readAllLines(file);
            Collections.sort(lines); // Sorting removes order variance
            String normalizedContent = String.join("\n", lines).replaceAll("\\s+", ""); // Remove whitespace

            // Compute hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalizedContent.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        }

    private String inferSupplierFromNamespace(String name) {
        Map<String, String> knownSuppliers = Map.ofEntries(
                Map.entry("com.unity", "Unity Technologies"),
                Map.entry("com.google", "Google"),
                Map.entry("org.apache", "Apache Software Foundation"),
                Map.entry("com.facebook", "Meta (Facebook)"),
                Map.entry("com.microsoft", "Microsoft"),
                Map.entry("org.springframework", "Spring Framework Team"),
                Map.entry("io.netty", "Netty Project"),
                Map.entry("org.hibernate", "Hibernate Team")
        );

        for (String prefix : knownSuppliers.keySet()) {
            if (name.startsWith(prefix)) {
                return knownSuppliers.get(prefix);
            }
        }

        return "Unknown";
    }

    //to extract the supplier name
    public String inferSupplierFromOsv(String name, String version) {
        HttpClient client = HttpClient.newHttpClient();
        String body = "{ \"package\": { \"name\": \"" + name + "\", \"ecosystem\": \"PyPI\" }, \"version\": \"" + version + "\" }";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.osv.dev/v1/query"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            JsonNode related = root.path("related");
            if (related.isArray()) {
                for (JsonNode entry : related) {
                    String repoUrl = entry.path("repo").asText();
                    if (repoUrl.contains("github.com")) {
                        return repoUrl.split("/")[3]; // get the GitHub owner/org
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback to namespace-based heuristics
        return inferSupplierFromNamespace(name);
    }

    private String computeSHA256(String normalizedContent) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(normalizedContent.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    //update the sbom
    @Transactional
    public void uploadUpdatedSbom(Device device, Sbom newSbom, List<SoftwarePackage> newPackages) {
        Long deviceId = device.getId();

        // 1. Mark existing SBOM archives as not latest
        List<SbomArchive> existingArchives = sbomArchiveRepository.findAllByDeviceId(deviceId);
        for (SbomArchive archive : existingArchives) {
            archive.setIsLatest(false);
            sbomArchiveRepository.save(archive);
        }

        // 2. Delete old SBOM and associated data
        Optional<Sbom> existingSbomOpt = sbomRepository.findByDeviceId(deviceId);
        if (existingSbomOpt.isPresent()) {
            Sbom oldSbom = existingSbomOpt.get();
            externalReferenceRepository.deleteBySbom_Id(oldSbom.getId());
            softwarePackageRepository.deleteByDeviceId(deviceId);
            sbomRepository.delete(oldSbom);
        }

        // 3. Save the new SBOM and its packages
        newSbom.setDevice(device);
        Sbom savedSbom = sbomRepository.save(newSbom);

        for (SoftwarePackage pkg : newPackages) {
            pkg.setDevice(device);
            pkg.setSbom(savedSbom);
        }
        softwarePackageRepository.saveAll(newPackages);

        // 4. Archive the new SBOM
        try {
            String jsonContent = sbomArchiveService.generateJsonSbomContent(device, savedSbom, newPackages);
            SbomArchive newArchive = new SbomArchive();
            newArchive.setDevice(device);
            newArchive.setVersion(savedSbom.getVersion());
            newArchive.setSbomContent(jsonContent);
            newArchive.setIsLatest(true);
            sbomArchiveRepository.save(newArchive);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SBOM JSON content", e);
        }
    }

}

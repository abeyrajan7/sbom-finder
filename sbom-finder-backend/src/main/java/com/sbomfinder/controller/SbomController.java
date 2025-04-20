package com.sbomfinder.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbomfinder.dto.RepoRequestDTO;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.ExternalReference;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.ExternalReferenceRepository;
import com.sbomfinder.repository.SbomRepository;
import com.sbomfinder.repository.SoftwarePackageRepository;
import com.sbomfinder.service.SbomService;
import com.sbomfinder.util.CustomMultipartFile;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/sboms")
public class SbomController {

    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;

    @Autowired
    private SbomRepository sbomRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    @Autowired
    private SbomService sbomService;

    @PostMapping("/upload-sbom")
    public ResponseEntity<String> uploadSbom(
            @RequestParam("sbomFile") MultipartFile sbomFile,
            @RequestParam("category") String category,
            @RequestParam(value = "deviceName", required = false) String deviceName,
            @RequestParam(value = "manufacturer", required = false) String manufacturer,
            @RequestParam(value = "operatingSystem", required = false) String operatingSystem,
            @RequestParam(value = "osVersion", required = false) String osVersion,
            @RequestParam(value = "kernelVersion", required = false) String kernelVersion
    )
    {
        try {
            JsonNode jsonNode = objectMapper.readTree(sbomFile.getInputStream());
            String result = sbomService.processSbomFromJson(jsonNode, category,
                    deviceName, manufacturer, operatingSystem, osVersion, kernelVersion);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error parsing SBOM: " + e.getMessage());
        }
    }

    @PostMapping("/from-repo")
    public ResponseEntity<String> generateSBOMFromRepo(@RequestBody RepoRequestDTO request) {
        String repoUrl = request.getRepoUrl();
        String category = request.getCategory();
        String deviceNameInput = request.getDeviceName();
        String manufacturer = request.getManufacturer();
        String operatingSystem = request.getOperatingSystem();
        String osVersion = request.getOsVersion();
        String kernelVersion = request.getKernelVersion();

        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("repo-sbom");

            // 1. Clone the GitHub repository
            Process clone = new ProcessBuilder("git", "clone", repoUrl, tempDir.toString())
                    .inheritIO()
                    .start();
            clone.waitFor();

            // 2. Search for dependency files
            List<Path> dependencyFiles = Files.walk(tempDir)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return
                                // Node.js
                                name.equals("package.json") ||
                                        // Java (Maven/Gradle)
                                        name.contains("pom") || name.contains("gradle") ||
                                        // Python
                                        name.contains("requirement") || name.contains("pipfile") || name.contains("setup") ||
                                        // Go
                                        name.equals("go.mod") ||
                                        // PHP
                                        name.equals("composer.json") ||
                                        // Rust
                                        name.equals("cargo.toml");
                    })
                    .collect(Collectors.toList());


            if (dependencyFiles.isEmpty()) {
                return ResponseEntity.badRequest().body("No supported dependency file found in repository.");
            }

            // 3. Create SBOM and Device FIRST
            String hash = DigestUtils.sha256Hex(repoUrl);
            String deviceName = (deviceNameInput != null && !deviceNameInput.isBlank())
                    ? deviceNameInput
                    : SbomService.extractRepoName(repoUrl);

            Sbom newSbom = new Sbom("Manual", "N/A", "N/A", "N/A",
                    LocalDateTime.now(), "GitHub", "Repo Upload", hash);
            Sbom savedSbom = sbomRepository.save(newSbom);

            Device newDevice = new Device(deviceName,
                    manufacturer != null ? manufacturer : "Unknown",
                    category != null ? category : "Unknown",
                    operatingSystem != null ? operatingSystem : "Unknown OS",
                    osVersion != null ? osVersion : "Unknown Version",
                    kernelVersion != null ? kernelVersion : "Unknown Kernel",
                    SbomService.extractDomain(repoUrl),
                    savedSbom);
            Device savedDevice = deviceRepository.save(newDevice);

            // 4. Now parse dependency files
            List<SoftwarePackage> allExtractedPackages = new ArrayList<>();

            for (Path depFile : dependencyFiles) {
                List<SoftwarePackage> extractedPackages = sbomService.extractPackagesFromDependencyFile(depFile, savedSbom, savedDevice);
                allExtractedPackages.addAll(extractedPackages);
            }

            if (allExtractedPackages.isEmpty()) {
                return ResponseEntity.badRequest().body("No dependencies found in detected files.");
            }

            // 5. Save extracted packages
            for (SoftwarePackage pkg : allExtractedPackages) {
                softwarePackageRepository.save(pkg);
                sbomService.checkAndSaveVulnerabilities(pkg);
            }

            return ResponseEntity.ok("SBOM created successfully from repository!");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    System.err.println("Failed to delete temp folder: " + e.getMessage());
                }
            }
        }
    }






    public static class DeviceDetailsResponse {
        public String deviceName;
        public String manufacturer;
        public String category;
        public String operatingSystem;
        public Sbom sbomDetails;
        public java.util.List<SoftwarePackage> softwarePackages;
        public java.util.List<ExternalReference> externalReferences;

        public DeviceDetailsResponse(Device device, Sbom sbom, java.util.List<SoftwarePackage> softwarePackages, java.util.List<ExternalReference> externalReferences) {
            this.deviceName = device.getDeviceName();
            this.manufacturer = device.getManufacturer();
            this.category = device.getCategory();
            this.operatingSystem = device.getOperatingSystem();
            this.sbomDetails = sbom;
            this.softwarePackages = softwarePackages;
            this.externalReferences = externalReferences;
        }
    }
}

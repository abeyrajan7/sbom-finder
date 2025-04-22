package com.sbomfinder.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbomfinder.dto.RepoRequestDTO;
import com.sbomfinder.service.SbomGenerationResult;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.ExternalReference;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.ExternalReferenceRepository;
import com.sbomfinder.repository.SbomRepository;
import com.sbomfinder.repository.SoftwarePackageRepository;
import com.sbomfinder.service.SbomService;
import com.sbomfinder.service.SbomGeneratorService;
import com.sbomfinder.util.CustomMultipartFile;
import com.sbomfinder.util.ArchiveUtils;


import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
@RequestMapping("/api/sboms")
public class SbomController {

    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;

    @Autowired
    private SbomRepository sbomRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    @Autowired
    private SbomGeneratorService sbomGeneratorService;

    @PostMapping("/from-repo")
    public ResponseEntity<String> generateSBOMFromRepo(@RequestBody RepoRequestDTO request) {
        String repoUrl = request.getRepoUrl();
        String deviceName = request.getDeviceName();
        String manufacturer = request.getManufacturer();
        String category = request.getCategory();
        String operatingSystem = request.getOperatingSystem();
        String osVersion = request.getOsVersion();
        String kernelVersion = request.getKernelVersion();

        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("repo-sbom");

            // 1. Clone the repo
            Process clone = new ProcessBuilder("git", "clone", repoUrl, tempDir.toString())
                    .inheritIO()
                    .start();
            clone.waitFor();

            // 2. Use common service
            sbomGeneratorService.generateSbomAndDeviceFromDirectory(
                    tempDir,
                    deviceName != null ? deviceName : SbomService.extractRepoName(repoUrl),
                    category != null ? category : "Unknown",
                    manufacturer,
                    operatingSystem != null ? operatingSystem : "Unknown OS",
                    osVersion != null ? osVersion : "Unknown Version",
                    kernelVersion != null ? kernelVersion : "Unknown Kernel",
                    "Repo Upload" // sourceType
            );

            return ResponseEntity.ok("SBOM created successfully from repository!");

        } catch (Exception e) {
            e.printStackTrace();
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


    @PostMapping("/upload-source")
    public ResponseEntity<?> uploadSourceZip(@RequestParam("file") MultipartFile file,
                                             @RequestParam("category") String category,
                                             @RequestParam(value = "deviceName", required = false) String deviceName,
                                             @RequestParam(value = "manufacturer", required = false) String manufacturer,
                                             @RequestParam(value = "operatingSystem", required = false) String operatingSystem,
                                             @RequestParam(value = "osVersion", required = false) String osVersion,
                                             @RequestParam(value = "kernelVersion", required = false) String kernelVersion) {
        try {
            // Save uploaded file properly
            String filename = file.getOriginalFilename().toLowerCase();
            Path tempFile = Files.createTempFile("upload-", filename.substring(filename.lastIndexOf('.')));
            file.transferTo(tempFile.toFile()); // Save only once

// Extract
            Path extractedDir = Files.createTempDirectory("extracted-source");

            if (filename.endsWith(".zip")) {
                ArchiveUtils.unzip(tempFile.toFile().getAbsolutePath(), extractedDir.toString());
            } else if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
                ArchiveUtils.extractTarGz(tempFile.toFile().getAbsolutePath(), extractedDir.toString());
            } else if (filename.endsWith(".tar")) {
                ArchiveUtils.extractTar(tempFile.toFile().getAbsolutePath(), extractedDir.toString());
            } else {
                throw new IllegalArgumentException("Unsupported file type: only .zip, .tar, .tar.gz supported");
            }

// 3. Use common service
            SbomGenerationResult result = sbomGeneratorService.generateSbomAndDeviceFromDirectory(
                    extractedDir,
                    deviceName,
                    category,
                    manufacturer,
                    operatingSystem,
                    osVersion,
                    kernelVersion,
                    "Source Upload"
            );
            Device device = result.getDevice();
            String version = result.getVersion();

            return ResponseEntity.ok("SBOM and device uploaded successfully! Device ID: " + device.getId() + ", Version: " + version);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error processing uploaded source: " + e.getMessage());
        }
    }

    @Transactional
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<String> deleteDevice(@PathVariable Long deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Device device = deviceOpt.get();

        // Fetch all SBOMs linked to this Device
        Optional<Sbom> sbomOpt = sbomRepository.findByDevice(device);
        if (sbomOpt.isPresent()) {
            Sbom sbom = sbomOpt.get();
            // 1. Delete Software Packages linked to this Device
            softwarePackageRepository.deleteByDeviceId(deviceId);

            // 2. Delete External References linked to this SBOM
            externalReferenceRepository.deleteBySbom_Id(sbom.getId());

            // 3. Delete the SBOM itself
            sbomRepository.delete(sbom);
        }

        // 4. Finally delete the Device
        deviceRepository.delete(device);

        return ResponseEntity.ok("Device and all associated SBOMs deleted successfully!");
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

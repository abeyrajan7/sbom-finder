package com.sbomfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.model.Supplier;


import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.service.DigitalFootprintService;
import com.sbomfinder.repository.SbomRepository;
import com.sbomfinder.repository.SoftwarePackageRepository;
import com.sbomfinder.util.GitHubReleaseFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sbomfinder.model.SbomArchive;
import com.sbomfinder.repository.SbomArchiveRepository;
import com.sbomfinder.repository.ExternalReferenceRepository;
import com.sbomfinder.repository.SupplierRepository;


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

    @Autowired
    private DigitalFootprintService digitalFootprintService;

    @Autowired
    private ExternalReferenceService externalReferenceService;

    @Autowired
    private SupplierRepository supplierRepository;

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
                    boolean isMatch = name.equals("package.json") ||
                            name.equals("package-lock.json") ||
                            name.equals("pom.xml") ||
                            name.equals("build.gradle") ||
                            name.equals("requirements.txt") ||
                            name.equals("pipfile") ||
                            name.equals("setup.py") ||
                            name.equals("go.mod") ||
                            name.equals("composer.json") ||
                            name.equals("cargo.toml");

                    if (isMatch) {
                        System.out.println("Found dependency file: " + path.toString());
                        try {
                            System.out.println("Content preview: " + Files.readString(path).substring(0, 100));
                        } catch (Exception e) {
                            System.out.println("Failed to read content from: " + path);
                        }
                    }

                    return isMatch;
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
            String footprint = String.join("\n\n", digitalFootprintService.generateDigitalFootprints(dependencyFiles));

            device = new Device(deviceName, manufacturer, category, operatingSystem, osVersion, kernelVersion, footprint);
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
            String ecosystem = determineEcosystemFromFile(depFile.getFileName().toString());

            for (SoftwarePackage pkg : extractedPackages) {
                softwarePackageRepository.save(pkg);
                sbomService.checkAndSaveVulnerabilities(pkg);

                String supplierName = inferSupplier(pkg.getName(), pkg.getVersion(), ecosystem);
                Supplier supplier = getOrCreateSupplier(supplierName);
                pkg.setSupplier(supplier);
                softwarePackageRepository.save(pkg);
                sbomService.checkAndSaveVulnerabilities(pkg);
            }

            allPackages.addAll(extractedPackages);
        }

        //save and extract external references
        List<String> extractedLinks = externalReferenceService.extractExternalReferences(extractedDir);
        externalReferenceService.saveExternalReferences(sbom, extractedLinks);

        return new SbomGenerationResult(version, device);
    }

    private String determineEcosystemFromFile(String fileName) {
        if (fileName.contains("requirement") || fileName.equalsIgnoreCase("pipfile") || fileName.equalsIgnoreCase("setup.py")) return "pypi";
        if (fileName.equalsIgnoreCase("package.json")) return "npm";
        if (fileName.equalsIgnoreCase("cargo.toml")) return "cargo";
        if (fileName.equalsIgnoreCase("pom.xml") || fileName.equalsIgnoreCase("build.gradle")) return "maven";
        return "unknown";
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

//    private String inferSupplierFromNamespace(String name) {
//        Map<String, String> knownSuppliers = Map.ofEntries(
//                Map.entry("com.unity", "Unity Technologies"),
//                Map.entry("com.google", "Google"),
//                Map.entry("org.apache", "Apache Software Foundation"),
//                Map.entry("com.facebook", "Meta (Facebook)"),
//                Map.entry("com.microsoft", "Microsoft"),
//                Map.entry("org.springframework", "Spring Framework Team"),
//                Map.entry("io.netty", "Netty Project"),
//                Map.entry("org.hibernate", "Hibernate Team")
//        );
//
//        for (String prefix : knownSuppliers.keySet()) {
//            if (name.startsWith(prefix)) {
//                return knownSuppliers.get(prefix);
//            }
//        }
//
//        return "Unknown";
//    }

    //to extract the supplier name
    public String inferSupplier(String name, String version, String ecosystem) {
        try {
            switch (ecosystem.toLowerCase()) {
                case "pypi": return getPypiSupplier(name);
                case "npm": return getNpmSupplier(name);
                case "cargo": return getCratesSupplier(name);
                case "maven":
                    String[] parts = name.split(":");
                    return parts.length == 2 ? getMavenSupplier(parts[0], parts[1]) : "Unknown";
                default: return "Unknown";
            }
        } catch (Exception e) {
            return "Unknown";
        }
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

    //method to save supplier info
    private Supplier getOrCreateSupplier(String name) {
        return supplierRepository.findByName(name)
                .orElseGet(() -> {
                    Supplier s = new Supplier();
                    s.setName(name);
                    return supplierRepository.save(s);
                });
    }

    private String getPypiSupplier(String name) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://pypi.org/pypi/" + name + "/json"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = new ObjectMapper().readTree(response.body());
                JsonNode info = root.path("info");

                if (info.hasNonNull("author") && !info.get("author").asText().isBlank()) {
                    return info.get("author").asText();
                }
                if (info.hasNonNull("maintainer") && !info.get("maintainer").asText().isBlank()) {
                    return info.get("maintainer").asText();
                }
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private String getNpmSupplier(String name) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://registry.npmjs.org/" + name))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                JsonNode latestTag = root.path("dist-tags").path("latest");
                if (latestTag.isMissingNode()) return "Unknown";

                JsonNode versionNode = root.path("versions").path(latestTag.asText()).path("author");
                if (versionNode.has("name")) return versionNode.get("name").asText();
                if (versionNode.isTextual()) return versionNode.asText();
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private String getCratesSupplier(String name) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://crates.io/api/v1/crates/" + name))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = new ObjectMapper().readTree(response.body());
                JsonNode crate = root.path("crate");

                String homepage = crate.path("homepage").asText();
                String repository = crate.path("repository").asText();

                return extractDomain(homepage != null ? homepage : repository);
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private String getMavenSupplier(String groupId, String artifactId) {
        try {
            String query = "https://search.maven.org/solrsearch/select?q=g:\"" + groupId + "\"+AND+a:\"" + artifactId + "\"&rows=1&wt=json";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(query))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = new ObjectMapper().readTree(response.body());
                JsonNode docs = root.path("response").path("docs");
                if (docs.isArray() && docs.size() > 0 && docs.get(0).has("publisher")) {
                    return docs.get(0).get("publisher").asText();
                }
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return (host != null) ? host.replace("www.", "") : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

}

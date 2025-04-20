package com.sbomfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbomfinder.dto.NormalizedSbomDataDTO;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.ExternalReference;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.model.Vulnerability;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.ExternalReferenceRepository;
import com.sbomfinder.repository.SbomRepository;
import com.sbomfinder.repository.SoftwarePackageRepository;
import com.sbomfinder.repository.VulnerabilityRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.Optional;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;

@Service
public class SbomService {

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;
    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;
    @Autowired
    private SbomRepository sbomRepository;
    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;
    @Autowired
    private DeviceRepository deviceRepository;

    public NormalizedSbomDataDTO parseSPDX(JsonNode jsonNode, String category,
                                           String deviceName, String manufacturer,
                                           String operatingSystem, String osVersion,
                                           String kernelVersion) {
        NormalizedSbomDataDTO data = new NormalizedSbomDataDTO();
        data.setFormat("SPDX");
        data.setSpecVersion(jsonNode.path("spdxVersion").asText());
        data.setDataLicense(jsonNode.path("dataLicense").asText());
        data.setDocumentNamespace(jsonNode.path("documentNamespace").asText());
        data.setCreatedTime(LocalDateTime.now());

        JsonNode creationInfo = jsonNode.path("creationInfo");
        if (creationInfo.has("creators") && creationInfo.path("creators").isArray()) {
            data.setVendor(creationInfo.path("creators").get(0).asText("Unknown Vendor"));
        } else {
            data.setVendor("Unknown Vendor");
        }

        data.setToolName(creationInfo.path("comment").asText("Unknown"));

        // Set device name
        String rawName = jsonNode.path("name").asText(null);
        String finalDeviceName = Optional.ofNullable(rawName)
                .filter(name -> !name.isEmpty())
                .map(SbomService::cleanDeviceName)
                .orElseGet(() -> deviceName != null && !deviceName.isEmpty()
                        ? cleanDeviceName(deviceName) : "Unknown Device");
        data.setDeviceName(finalDeviceName);

        // Set manufacturer
        String finalManufacturer = (manufacturer != null && !manufacturer.isEmpty())
                ? manufacturer : "Unknown Manufacturer";
        data.setManufacturer(finalManufacturer);

        // Set operating system
        String finalOs = (operatingSystem != null && !operatingSystem.isEmpty())
                ? operatingSystem : "Unknown OS";
        data.setOperatingSystem(finalOs);

        // Set OS version
        String finalOsVersion = (osVersion != null && !osVersion.isEmpty())
                ? osVersion : "Unknown Version";
        data.setOsVersion(finalOsVersion);

        // Set kernel version
        String finalKernelVersion = (kernelVersion != null && !kernelVersion.isEmpty())
                ? kernelVersion : "Unknown Kernel";
        data.setKernelVersion(finalKernelVersion);

        data.setDigitalFootprint("Not Available");

        List<JsonNode> packagesList = new ArrayList<>();
        JsonNode packagesNode = jsonNode.path("packages");
        if (packagesNode.isArray()) {
            packagesList = StreamSupport.stream(packagesNode.spliterator(), false)
                    .collect(Collectors.toList());
        }
        data.setPackages(packagesList);

        List<JsonNode> externalRefsList = new ArrayList<>();
        JsonNode externalRefsNode = jsonNode.path("externalDocumentRefs");
        if (externalRefsNode.isArray()) {
            externalRefsList = StreamSupport.stream(externalRefsNode.spliterator(), false)
                    .collect(Collectors.toList());
        }
        data.setExternalReferences(externalRefsList);

        data.setCategory(category);
        return data;
    }



    public NormalizedSbomDataDTO parseCycloneDX(JsonNode jsonNode, String category,
                                                String deviceName, String manufacturer,
                                                String operatingSystem, String osVersion,
                                                String kernelVersion) {
        NormalizedSbomDataDTO data = new NormalizedSbomDataDTO();
        data.setFormat("CycloneDX");
        data.setSpecVersion(jsonNode.path("specVersion").asText(""));
        data.setDataLicense(jsonNode.path("dataLicense").asText(""));
        data.setDocumentNamespace(jsonNode.path("documentNamespace").asText(""));

        JsonNode metadataNode = jsonNode.path("metadata");

        if (metadataNode.has("timestamp")) {
            String timestamp = metadataNode.path("timestamp").asText();
            if (timestamp != null && !timestamp.isEmpty()) {
                data.setCreatedTime(OffsetDateTime.parse(timestamp).toLocalDateTime());
            }
        }

        JsonNode toolsArray = metadataNode.path("tools");
        if (toolsArray != null && toolsArray.isArray() && toolsArray.size() > 0) {
            JsonNode toolNode = toolsArray.get(0);
            data.setVendor(toolNode.path("vendor").asText("Unknown Vendor"));
            data.setToolName(toolNode.path("name").asText("Unknown Tool"));
        } else {
            data.setVendor("Unknown Vendor");
            data.setToolName("Unknown Tool");
        }

        // Set device name
        JsonNode component = metadataNode.path("component");
        String rawName = component.path("name").asText(null);

// Check if rawName is not a local file path
        boolean isInvalidPath = rawName != null && (rawName.contains(":\\") || rawName.contains("/") || rawName.contains("\\"));

        String finalDeviceName = (!isInvalidPath && rawName != null && !rawName.isEmpty())
                ? cleanDeviceName(rawName)
                : (deviceName != null && !deviceName.isEmpty() ? cleanDeviceName(deviceName) : "Unknown Device");

        data.setDeviceName(finalDeviceName);


// Set manufacturer
        String rawManufacturer = component.path("manufacturer").asText(null);
        String finalManufacturer = (rawManufacturer != null && !rawManufacturer.isEmpty())
                ? rawManufacturer
                : (manufacturer != null && !manufacturer.isEmpty() ? manufacturer : "Unknown Manufacturer");
        data.setManufacturer(finalManufacturer);

// Set operating system
        String rawOs = component.path("operatingSystem").asText(null);
        String finalOs = (rawOs != null && !rawOs.isEmpty())
                ? rawOs
                : (operatingSystem != null && !operatingSystem.isEmpty() ? operatingSystem : "Unknown OS");
        data.setOperatingSystem(finalOs);

// Set OS version
        String rawOsVersion = component.path("version").asText(null);
        String finalOsVersion = (rawOsVersion != null && !rawOsVersion.isEmpty())
                ? rawOsVersion
                : (osVersion != null && !osVersion.isEmpty() ? osVersion : "Unknown Version");
        data.setOsVersion(finalOsVersion);

// Set kernel version
        String rawKernelVersion = component.path("kernel").asText(null);
        String finalKernelVersion = (rawKernelVersion != null && !rawKernelVersion.isEmpty())
                ? rawKernelVersion
                : (kernelVersion != null && !kernelVersion.isEmpty() ? kernelVersion : "Unknown Kernel");
        data.setKernelVersion(finalKernelVersion);

        Set<String> footprintSet = new HashSet<>();

        JsonNode externalRefs = jsonNode.path("externalReferences");
        if (externalRefs.isArray()) {
            for (JsonNode ref : externalRefs) {
                String locator = ref.path("referenceLocator").asText();
                String domain = extractDomain(locator);
                if (!domain.isEmpty()) footprintSet.add(domain);
            }
        }

        JsonNode components = jsonNode.path("components");
        if (components.isArray()) {
            for (JsonNode comp : components) {
                String downloadUrl = comp.path("downloadLocation").asText();
                String domain = extractDomain(downloadUrl);
                if (!domain.isEmpty()) footprintSet.add(domain);

                JsonNode extRefs = comp.path("externalRefs");
                if (extRefs.isArray()) {
                    for (JsonNode ref : extRefs) {
                        String locator = ref.path("referenceLocator").asText();
                        if (locator.startsWith("pkg:")) {
                            String[] parts = locator.split("/");
                            if (parts.length > 1) footprintSet.add(parts[0].replace("pkg:", ""));
                        } else {
                            String urlDomain = extractDomain(locator);
                            if (!urlDomain.isEmpty()) footprintSet.add(urlDomain);
                        }
                    }
                }
            }
        }

        JsonNode toolsArrayMetadata = metadataNode.path("tools");
        if (toolsArrayMetadata.isArray()) {
            for (JsonNode tool : toolsArrayMetadata) {
                String toolName = tool.path("name").asText(null);
                if (toolName != null && !toolName.isEmpty()) footprintSet.add(toolName);
                String vendor = tool.path("vendor").asText(null);
                if (vendor != null && !vendor.isEmpty()) footprintSet.add(vendor);
            }
        }

        if (footprintSet.isEmpty()) {
            data.setDigitalFootprint("Not Available");
        } else {
            data.setDigitalFootprint(String.join(", ", footprintSet));
        }

        List<JsonNode> componentsList = new ArrayList<>();
        JsonNode componentsNode = jsonNode.path("components");
        if (componentsNode.isArray()) {
            componentsList = StreamSupport.stream(componentsNode.spliterator(), false).collect(Collectors.toList());
        }
        data.setPackages(componentsList);

        List<JsonNode> externalRefsList = new ArrayList<>();
        JsonNode extRefsNode = jsonNode.path("externalReferences");
        if (extRefsNode.isArray()) {
            externalRefsList = StreamSupport.stream(extRefsNode.spliterator(), false).collect(Collectors.toList());
        }
        data.setExternalReferences(externalRefsList);

        data.setCategory(category);
        return data;
    }

    public String processSbomFromJson(JsonNode jsonNode, String category,
                                      String deviceName, String manufacturer,
                                      String operatingSystem, String osVersion,
                                      String kernelVersion)
    {
        String format = detectFormat(jsonNode);

        NormalizedSbomDataDTO sbomData;
        switch (format) {
            case "cyclonedx":
                sbomData = parseCycloneDX(jsonNode, category,
                        deviceName, manufacturer, operatingSystem,
                        osVersion, kernelVersion);
                break;
            case "spdx":
                sbomData = parseSPDX(jsonNode, category,
                        deviceName, manufacturer, operatingSystem,
                        osVersion, kernelVersion);
                break;
            default:
                throw new IllegalArgumentException("Unsupported SBOM format");
        }

        String hash = generateHash(jsonNode.toString());
        if (sbomRepository.existsByHash(hash)) {
            throw new IllegalStateException("SBOM already exists.");
        }

        Sbom newSbom = new Sbom(sbomData.getFormat(), sbomData.getSpecVersion(), sbomData.getDataLicense(),
                sbomData.getDocumentNamespace(), sbomData.getCreatedTime(),
                sbomData.getVendor(), sbomData.getToolName(), hash);
        sbomRepository.save(newSbom);

        for (JsonNode ref : sbomData.getExternalReferences()) {
            ExternalReference extRef = new ExternalReference(newSbom,
                    ref.path("referenceCategory").asText(""),
                    ref.path("referenceType").asText(""),
                    ref.path("referenceLocator").asText(""));
            externalReferenceRepository.save(extRef);
        }

        Device device = deviceRepository.findByDeviceNameAndManufacturer(
                sbomData.getDeviceName(), sbomData.getManufacturer()
        ).orElseGet(() -> {
            Device newDevice = new Device(
                    sbomData.getDeviceName(),
                    sbomData.getManufacturer(),
                    sbomData.getCategory(),
                    sbomData.getOperatingSystem(),
                    sbomData.getOsVersion(),
                    sbomData.getKernelVersion(),
                    sbomData.getDigitalFootprint(),
                    newSbom
            );
            return deviceRepository.save(newDevice);
        });

        if ("spdx".equals(format)) {
            processSpdxPackages(sbomData.getPackages(), newSbom, device);
        } else {
            processCycloneDXPackages(sbomData.getPackages(), newSbom, device);
        }

        return "SBOM parsed and stored successfully for format: " + format;
    }


    public String detectFormat(JsonNode jsonNode) {
        if (jsonNode.has("bomFormat") && "CycloneDX".equalsIgnoreCase(jsonNode.get("bomFormat").asText())) {
            return "cyclonedx";
        } else if (jsonNode.has("spdxVersion")) {
            return "spdx";
        }
        return "unknown";
    }


    public String generateHash(String content) {
        return DigestUtils.sha256Hex(content);
    }




    public void processSpdxPackages(List<JsonNode> packagesList, Sbom sbom, Device device) {
        for (JsonNode pkg : packagesList) {
            String name = pkg.path("name").asText();
            String version = pkg.path("versionInfo").asText(null);
            String purl = "";

            if (pkg.has("externalRefs")) {
                for (JsonNode ref : pkg.path("externalRefs")) {
                    if ("purl".equals(ref.path("referenceType").asText())) {
                        purl = ref.path("referenceLocator").asText();
                        break;
                    }
                }
            }

            SoftwarePackage softwarePackage = new SoftwarePackage();
            softwarePackage.setName(name);
            softwarePackage.setVersion(version != null ? version : "Unknown");
            softwarePackage.setPurl(purl);
            softwarePackage.setSbom(sbom);
            softwarePackage.setDevice(device);
            softwarePackageRepository.save(softwarePackage);
            checkAndSaveVulnerabilities(softwarePackage);
        }
    }


    public void processCycloneDXPackages(List<JsonNode> packages, Sbom newSbom, Device device) {
        for (JsonNode pkg : packages) {
            String name = pkg.path("name").asText();
            String version = pkg.has("version") ? pkg.path("version").asText(null) : null;

            if (!softwarePackageRepository.existsByNameAndVersion(name, version)) {
                SoftwarePackage sp = new SoftwarePackage(newSbom, name, version,
                        pkg.path("publisher").asText(null),
                        pkg.path("downloadLocation").asText(null),
                        pkg.path("licenses").size() > 0 ? pkg.path("licenses").get(0).path("license").path("id").asText(null) : null,
                        null,
                        pkg.path("copyrightText").asText(null),
                        pkg.path("type").asText(null)
                );
                sp.setDevice(device);
                sp.setPurl(pkg.path("purl").asText(null)); 
                softwarePackageRepository.save(sp);
                checkAndSaveVulnerabilities(sp);
                
            }
        }
    }

    public List<Vulnerability> fetchVulnerabilitiesFromOsv(SoftwarePackage pkg) {
        String name = extractNameFromPurl(pkg.getPurl(), pkg.getName());
        String version = pkg.getVersion();
        String ecosystem = extractEcosystemFromPurl(pkg.getPurl());
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> payload = new HashMap<>();
        Map<String, String> packageMap = new HashMap<>();

        packageMap.put("name", name);
        packageMap.put("ecosystem", ecosystem);
        payload.put("package", packageMap);
        payload.put("version", version);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    "https://api.osv.dev/v1/query",
                    request,
                    JsonNode.class
            );

            List<Vulnerability> vulnerabilities = new ArrayList<>();

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode vulns = response.getBody().path("vulns");
                for (JsonNode vuln : vulns) {
                    String id = vuln.path("id").asText();
                    String summary = vuln.path("summary").asText("No description available");

                    // Now, separately extract severity and score properly
                    String severityText = "Unknown";
                    Double score = 0.0;
                    JsonNode severityArray = vuln.path("severity");
                    if (severityArray.isArray() && severityArray.size() > 0) {
                        JsonNode severityNode = severityArray.get(0);
                        severityText = severityNode.path("type").asText("");
                        String scoreStr = severityNode.path("score").asText("");
                        if (!scoreStr.isEmpty()) {
                            try {
                                score = Double.parseDouble(scoreStr);
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    String sourceUrl = "";
                    if (vuln.has("references")) {
                        for (JsonNode ref : vuln.path("references")) {
                            if (ref.has("url")) {
                                sourceUrl = ref.path("url").asText();
                                break;
                            }
                        }
                    }

                    Vulnerability v = new Vulnerability();
                    v.setCveId(id);
                    v.setDescription(summary);
                    v.setSeverity(severityText);
                    v.setSourceUrl(sourceUrl);
                    v.setCvssScore(score);

                    vulnerabilities.add(v);
                }
            }

            return vulnerabilities;

        } catch (Exception e) {
            System.err.println("Error calling OSV API for " + name + "@" + version + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }


    public void checkAndSaveVulnerabilities(SoftwarePackage softwarePackage) {
        if (softwarePackage.getId() == null) {
            softwarePackage = softwarePackageRepository.save(softwarePackage);
        }

        List<Vulnerability> fetchedVulns = fetchVulnerabilitiesFromOsv(softwarePackage);

        Set<Vulnerability> linkedVulns = new HashSet<>();
        for (Vulnerability v : fetchedVulns) {
            Vulnerability existing = vulnerabilityRepository.findByCveId(v.getCveId())
                    .orElseGet(() -> vulnerabilityRepository.save(v));
            linkedVulns.add(existing);
        }

        // Link and save again
        softwarePackage.setVulnerabilities(linkedVulns);
        softwarePackageRepository.save(softwarePackage);
    }

    private String extractNameFromPurl(String purl, String fallbackName) {
        if (purl != null && purl.contains("/")) {
            return purl.substring(purl.lastIndexOf("/") + 1).split("@")[0];
        }
        return fallbackName;
    }

    private String extractEcosystemFromPurl(String purl) {
        if (purl == null || purl.isEmpty()) return "Unknown";
        try {
            if (purl.startsWith("pkg:")) {
                String purlBody = purl.substring(4);
                String[] parts = purlBody.split("/");
                String type = parts[0];
                switch (type.toLowerCase()) {
                    case "pypi": return "PyPI";
                    case "npm": return "npm";
                    case "maven": return "Maven";
                    case "golang": return "Go";
                    case "nuget": return "NuGet";
                    case "composer": return "Composer";
                    case "cargo": return "crates.io";
                    case "rubygems": return "RubyGems";
                    default: return type;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing purl: " + purl);
        }

        return "Unknown";
    }

    public static String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null ? host.replace("www.", "") : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String cleanDeviceName(String rawName) {
        if (rawName == null || rawName.isBlank()) return "Unknown Device";
        String cleaned = rawName.contains("/")
                ? rawName.substring(rawName.lastIndexOf("/") + 1)
                : rawName;
        return cleaned.replace("-", " ").trim();
    }

    public boolean fileAlreadyExists(MultipartFile file) throws IOException {
        String hash = DigestUtils.sha256Hex(file.getInputStream());
        return sbomRepository.existsByHash(hash);
    }

    public static String extractRepoName(String url) {
        if (url == null || url.isEmpty()) return "UnknownDevice";
        String[] parts = url.split("/");
        String repo = parts[parts.length - 1];
        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }
        // Convert snake_case to CamelCase
        String[] words = repo.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1));
            }
        }
        return sb.toString();  // like HaXiaomiHome
    }

    public List<SoftwarePackage> extractPackagesFromDependencyFile(Path filePath, Sbom sbom, Device device) {
    List<SoftwarePackage> packages = new ArrayList<>();
    String fileName = filePath.getFileName().toString().toLowerCase();

    try {
        String content = Files.readString(filePath);

        if (fileName.equals("package.json")) {
            // Node.js
            JsonNode json = new ObjectMapper().readTree(content);
            JsonNode deps = json.path("dependencies");
            if (deps.isObject()) {
                deps.fields().forEachRemaining(field -> {
                    SoftwarePackage sp = new SoftwarePackage();
                    sp.setName(field.getKey());
                    sp.setVersion(field.getValue().asText("Unknown"));
                    sp.setSbom(sbom);
                    sp.setDevice(device);
                    sp.setPurl(generatePurl("npm", sp.getName(), sp.getVersion()));
                    packages.add(sp);
                });
            }
        } else if (fileName.contains("requirement") || fileName.equals("pipfile")) {
            // Python (requirements.txt, Pipfile)
            Arrays.stream(content.split("\n"))
                  .filter(line -> !line.trim().isEmpty())
                  .forEach(line -> {
                      String[] parts = line.split("==");
                      String name = parts[0].trim();
                      String version = parts.length > 1 ? parts[1].trim() : "Unknown";
                      SoftwarePackage sp = new SoftwarePackage();
                      sp.setName(name);
                      sp.setVersion(version);
                      sp.setSbom(sbom);
                      sp.setDevice(device);
                      sp.setPurl(generatePurl("pypi", sp.getName(), sp.getVersion()));
                      packages.add(sp);
                  });
        } else if (fileName.contains("setup")) {
            // Python setup.py
            Matcher matcher = Pattern.compile("'([^']+)==([^']+)'").matcher(content);
            while (matcher.find()) {
                SoftwarePackage sp = new SoftwarePackage();
                sp.setName(matcher.group(1));
                sp.setVersion(matcher.group(2));
                sp.setSbom(sbom);
                sp.setDevice(device);
                sp.setPurl(generatePurl("pypi", sp.getName(), sp.getVersion()));
                packages.add(sp);
            }
        } else if (fileName.contains("pom")) {
            // Java Maven
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(filePath.toFile());
            NodeList dependencies = doc.getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Node dependency = dependencies.item(i);
                if (dependency.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) dependency;
                    String groupId = elem.getElementsByTagName("groupId").item(0).getTextContent();
                    String artifactId = elem.getElementsByTagName("artifactId").item(0).getTextContent();
                    String version = elem.getElementsByTagName("version").item(0) != null
                            ? elem.getElementsByTagName("version").item(0).getTextContent()
                            : "Unknown";
                    SoftwarePackage sp = new SoftwarePackage();
                    sp.setName(groupId + ":" + artifactId);
                    sp.setVersion(version);
                    sp.setSbom(sbom);
                    sp.setDevice(device);
                    sp.setPurl(generatePurl("maven", sp.getName(), sp.getVersion()));
                    packages.add(sp);
                }
            }
        } else if (fileName.contains("gradle")) {
            // Java Gradle
            Matcher matcher = Pattern.compile("implementation ['\"]([^:'\"]+):([^:'\"]+):([^'\"]+)['\"]").matcher(content);
            while (matcher.find()) {
                SoftwarePackage sp = new SoftwarePackage();
                sp.setName(matcher.group(1) + ":" + matcher.group(2));
                sp.setVersion(matcher.group(3));
                sp.setSbom(sbom);
                sp.setDevice(device);
                sp.setPurl(generatePurl("maven", sp.getName(), sp.getVersion()));
                packages.add(sp);
            }
        } else if (fileName.equals("go.mod")) {
            // Golang
            Arrays.stream(content.split("\n"))
                  .filter(line -> !line.trim().isEmpty() && (line.startsWith("\t") || line.contains(" ")))
                  .forEach(line -> {
                      String[] parts = line.trim().split("\\s+");
                      if (parts.length >= 2) {
                          SoftwarePackage sp = new SoftwarePackage();
                          sp.setName(parts[0]);
                          sp.setVersion(parts[1]);
                          sp.setSbom(sbom);
                          sp.setDevice(device);
                          sp.setPurl(generatePurl("golang", sp.getName(), sp.getVersion()));
                          packages.add(sp);
                      }
                  });
        } else if (fileName.equals("composer.json")) {
            // PHP Composer
            JsonNode json = new ObjectMapper().readTree(content);
            JsonNode deps = json.path("require");
            if (deps.isObject()) {
                deps.fields().forEachRemaining(field -> {
                    SoftwarePackage sp = new SoftwarePackage();
                    sp.setName(field.getKey());
                    sp.setVersion(field.getValue().asText("Unknown"));
                    sp.setSbom(sbom);
                    sp.setDevice(device);
                    sp.setPurl(generatePurl("composer", sp.getName(), sp.getVersion()));
                    packages.add(sp);
                });
            }
        } else if (fileName.equals("cargo.toml")) {
            // Rust Cargo
            Matcher matcher = Pattern.compile("^\\s*([^\\s=]+)\\s*=\\s*\"([^\"]+)\"").matcher(content);
            while (matcher.find()) {
                SoftwarePackage sp = new SoftwarePackage();
                sp.setName(matcher.group(1));
                sp.setVersion(matcher.group(2));
                sp.setSbom(sbom);
                sp.setDevice(device);
                sp.setPurl(generatePurl("cargo", sp.getName(), sp.getVersion()));
                packages.add(sp);
            }
        }

    } catch (Exception e) {
        System.err.println("Error extracting dependencies from file: " + fileName + ", " + e.getMessage());
    }

    return packages;
}

private String generatePurl(String ecosystem, String name, String version) {
    if (ecosystem == null || name == null) return null;
    version = (version != null && !version.isBlank()) ? "@" + version : "";
    switch (ecosystem.toLowerCase()) {
        case "npm":
            return "pkg:npm/" + name + version;
        case "maven":
            return "pkg:maven/" + name + version;
        case "pypi":
            return "pkg:pypi/" + name + version;
        case "composer":
            return "pkg:composer/" + name + version;
        case "cargo":
            return "pkg:cargo/" + name + version;
        case "golang":
            return "pkg:golang/" + name + version;
        default:
            return null;
    }
}






}

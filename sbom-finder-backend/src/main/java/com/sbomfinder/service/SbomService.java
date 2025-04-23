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

    public String generateHash(String content) {
        return DigestUtils.sha256Hex(content);
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
                    v.setSeverityLevel(calculateSeverityLevel(score));

                    vulnerabilities.add(v);
                }
            }

            return vulnerabilities;

        } catch (Exception e) {
            System.err.println("Error calling OSV API for " + name + "@" + version + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String calculateSeverityLevel(Double score) {
        if (score == null) return "Unknown";
        if (score == 0.0) return "None";
        if (score <= 3.9) return "Low";
        if (score <= 6.9) return "Medium";
        if (score <= 8.9) return "High";
        return "Critical";
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

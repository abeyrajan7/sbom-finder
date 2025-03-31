package com.sbomfinder.service;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.Device;
import com.sbomfinder.repository.SoftwarePackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sbomfinder.model.SoftwarePackage;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.sbomfinder.model.Vulnerability;
import com.sbomfinder.repository.VulnerabilityRepository;
import java.net.URI;
import java.net.URISyntaxException;
import com.sbomfinder.dto.VulnerabilityDTO;
import java.util.stream.Collectors;


@Service
public class SbomService {

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;
    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;

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

                    // Optional: Severity
                    String severity = "Unknown";
                    JsonNode severityArray = vuln.path("severity");
                    if (severityArray.isArray() && severityArray.size() > 0) {
                        JsonNode severityNode = severityArray.get(0);
                        severity = severityNode.path("type").asText("") + ": " + severityNode.path("score").asText("");
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
                    v.setSeverity(severity);
                    v.setSourceUrl(sourceUrl);

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


    private String extractMavenNameFromPurl(String purl) {
        if (purl != null && purl.startsWith("pkg:maven/")) {
            try {
                String[] parts = purl.replace("pkg:maven/", "").split("@")[0].split("/");
                if (parts.length == 2) {
                    return parts[0] + ":" + parts[1];
                }
            } catch (Exception e) {
                System.err.println("Error parsing purl: " + purl);
            }
        }

        return null;
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

}

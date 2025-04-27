package com.sbomfinder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbomfinder.dto.UnifiedSbomData;
import com.sbomfinder.dto.UnifiedComponent;
import com.sbomfinder.dto.UnifiedVulnerability;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.model.Vulnerability;

import com.sbomfinder.model.SbomArchive;
import org.springframework.stereotype.Service;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.repository.SbomArchiveRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SbomArchiveService {
    private final SbomArchiveRepository sbomArchiveRepository;

    public SbomArchiveService(SbomArchiveRepository sbomArchiveRepository) {
        this.sbomArchiveRepository = sbomArchiveRepository;
    }

    public void saveToArchive(Sbom sbom, Device device, String version, List<SoftwarePackage> softwarePackages) {
        try {
            // Mark previous archives as not latest
            List<SbomArchive> existingArchives = sbomArchiveRepository.findAllByDeviceId(device.getId());
            for (SbomArchive existing : existingArchives) {
                existing.setIsLatest(false);
                sbomArchiveRepository.save(existing);
            }

            // Generate JSON content
            String jsonContent = generateJsonSbomContent(device, sbom, softwarePackages);

            // Save new archive
            SbomArchive newArchive = new SbomArchive();
            newArchive.setDevice(device);
            newArchive.setVersion(version);
            newArchive.setSbomContent(jsonContent);
            newArchive.setIsLatest(true);
            sbomArchiveRepository.save(newArchive);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String generateJsonSbomContent(Device device, Sbom sbom, List<SoftwarePackage> packages) throws Exception {
        UnifiedSbomData sbomData = new UnifiedSbomData();
        sbomData.setDeviceName(device.getDeviceName());
        sbomData.setVersion(sbom.getVersion());
        if (packages == null) {
            packages = new ArrayList<>();
        }
        List<UnifiedComponent> components = packages.stream()
                .map(pkg -> new UnifiedComponent(pkg.getName(), pkg.getVersion(),
                        pkg.getPurl() != null ? pkg.getPurl() : "NOASSERTION"))
                .collect(Collectors.toList());
        sbomData.setComponents(components);

        List<UnifiedVulnerability> vulnerabilities = new ArrayList<>();
        for (SoftwarePackage pkg : packages) {
            if (pkg.getVulnerabilities() != null) {
                for (Vulnerability vuln : pkg.getVulnerabilities()) {
                    UnifiedVulnerability uv = new UnifiedVulnerability();
                    uv.setCveId(vuln.getCveId());
                    uv.setCvssScore(vuln.getCvssScore());
                    uv.setSeverity(vuln.getSeverity());
                    uv.setAffectedComponents(List.of(pkg.getPurl() != null ? pkg.getPurl() : pkg.getName()));
                    vulnerabilities.add(uv);
                }
            }
        }
        sbomData.setVulnerabilities(vulnerabilities);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sbomData);
    }

    public String generateCycloneDxJson(UnifiedSbomData sbomData) throws Exception {
        Map<String, Object> sbom = new LinkedHashMap<>();
        sbom.put("bomFormat", "CycloneDX");
        sbom.put("specVersion", "1.4");
        sbom.put("version", 1);

        List<Map<String, Object>> components = sbomData.getComponents().stream()
                .map(comp -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("type", "library");
                    map.put("name", comp.getName());
                    map.put("version", comp.getVersion());
                    map.put("purl", comp.getPurl() != null ? comp.getPurl() : "NOASSERTION");
                    return map;
                })
                .collect(Collectors.toList());

        sbom.put("components", components);

        if (sbomData.getVulnerabilities() != null && !sbomData.getVulnerabilities().isEmpty()) {
            List<Map<String, Object>> vulnerabilities = new ArrayList<>();

            for (UnifiedVulnerability vuln : sbomData.getVulnerabilities()) {
                Map<String, Object> vulnMap = new LinkedHashMap<>();
                vulnMap.put("id", vuln.getCveId());
                vulnMap.put("source", Map.of("name", "NVD"));

                Map<String, Object> rating = new LinkedHashMap<>();
                rating.put("score", vuln.getCvssScore() != null ? vuln.getCvssScore() : 0.0);
                rating.put("severity", vuln.getSeverity() != null ? vuln.getSeverity() : "UNKNOWN");
                rating.put("method", "CVSSv3");
                rating.put("vector", "NOASSERTION");

                vulnMap.put("ratings", List.of(rating));
                vulnMap.put("affects", vuln.getAffectedComponents().stream()
                        .map(ref -> Map.of("ref", ref))
                        .collect(Collectors.toList()));

                vulnerabilities.add(vulnMap);
            }

            sbom.put("vulnerabilities", vulnerabilities);
        }

        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sbom);
    }

    public String generateSpdxJson(UnifiedSbomData sbomData) throws Exception {
        Map<String, Object> sbom = new LinkedHashMap<>();
        sbom.put("spdxVersion", "SPDX-2.2");
        sbom.put("SPDXID", "SPDXRef-DOCUMENT");
        sbom.put("name", sbomData.getDeviceName());

        List<Map<String, Object>> packages = sbomData.getComponents().stream()
                .map(pkg -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("SPDXID", "SPDXRef-Package-" + pkg.getName().replaceAll("[^a-zA-Z0-9]", ""));
                    map.put("name", pkg.getName());
                    map.put("versionInfo", pkg.getVersion() != null ? pkg.getVersion() : "NOASSERTION");
                    map.put("downloadLocation", "NOASSERTION");
                    return map;
                })
                .collect(Collectors.toList());
        sbom.put("packages", packages);

        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sbom);
    }

}

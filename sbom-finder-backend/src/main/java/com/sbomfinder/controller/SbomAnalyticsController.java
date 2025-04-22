package com.sbomfinder.controller;

import com.sbomfinder.model.Device;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.model.Vulnerability;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.SoftwarePackageRepository;
import com.sbomfinder.repository.VulnerabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class SbomAnalyticsController {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @GetMapping("/operating-systems")
    public ResponseEntity<List<Map<String, Object>>> getOperatingSystems() {
        List<Device> devices = deviceRepository.findAll();
        Map<String, Long> counts = devices.stream()
                .collect(Collectors.groupingBy(
                        device -> Optional.ofNullable(device.getOperatingSystem()).orElse("Unknown"),
                        Collectors.counting()
                ));

        List<Map<String, Object>> response = counts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("sboms", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/suppliers")
    public ResponseEntity<List<Map<String, Object>>> getSuppliers() {
        List<Device> devices = deviceRepository.findAll();
        Map<String, Long> counts = devices.stream()
                .collect(Collectors.groupingBy(
                        device -> Optional.ofNullable(device.getManufacturer()).orElse("Unknown"),
                        Collectors.counting()
                ));

        List<Map<String, Object>> response = counts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("sboms", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/manufacturers")
    public ResponseEntity<List<Map<String, Object>>> getManufacturers() {
        return getSuppliers();
    }

    @GetMapping("/vulnerabilities-by-category")
    public ResponseEntity<List<Map<String, Object>>> getVulnerabilitiesByCategory() {
        List<Device> devices = deviceRepository.findAll();
        Map<String, Long> categoryVulnCounts = new HashMap<>();

        for (Device device : devices) {
            List<SoftwarePackage> packages = softwarePackageRepository.findByDeviceId(device.getId());
            long vulnCount = packages.stream()
                    .flatMap(pkg -> pkg.getVulnerabilities().stream())
                    .count();

            String category = Optional.ofNullable(device.getCategory()).orElse("Unknown");
            categoryVulnCounts.put(category, categoryVulnCounts.getOrDefault(category, 0L) + vulnCount);
        }

        List<Map<String, Object>> response = categoryVulnCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("value", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-vulnerable-packages")
    public ResponseEntity<List<Map<String, Object>>> getTopVulnerablePackages() {
        List<SoftwarePackage> packages = softwarePackageRepository.findAll();
        Map<String, Long> vulnCounts = new HashMap<>();

        for (SoftwarePackage pkg : packages) {
            long count = pkg.getVulnerabilities().size();
            vulnCounts.put(pkg.getName(), vulnCounts.getOrDefault(pkg.getName(), 0L) + count);
        }

        List<Map<String, Object>> response = vulnCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("vulns", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vulnerability-severity")
    public ResponseEntity<List<Map<String, Object>>> getVulnerabilitySeverity() {
        List<Vulnerability> vulnerabilities = vulnerabilityRepository.findAll();
        Map<String, Long> severityCounts = vulnerabilities.stream()
                .collect(Collectors.groupingBy(
                        v -> Optional.ofNullable(v.getSeverity()).orElse("Unknown"),
                        Collectors.counting()
                ));

        List<Map<String, Object>> response = severityCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("value", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vulnerable-suppliers")
    public ResponseEntity<List<Map<String, Object>>> getVulnerableSuppliers() {
        List<Device> devices = deviceRepository.findAll();
        Map<String, Long> supplierVulnCounts = new HashMap<>();

        for (Device device : devices) {
            List<SoftwarePackage> packages = softwarePackageRepository.findByDeviceId(device.getId());
            long vulnCount = packages.stream()
                    .flatMap(pkg -> pkg.getVulnerabilities().stream())
                    .count();

            String supplier = Optional.ofNullable(device.getManufacturer()).orElse("Unknown");
            supplierVulnCounts.put(supplier, supplierVulnCounts.getOrDefault(supplier, 0L) + vulnCount);
        }

        List<Map<String, Object>> response = supplierVulnCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("vulns", entry.getValue());
                    return map;
                })
                .sorted((a, b) -> Long.compare((long) b.get("vulns"), (long) a.get("vulns")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category")
    public List<Map<String, Object>> getFixedCategoriesAnalytics() {
        List<Map<String, Object>> response = new ArrayList<>();

        // Fetch count for Fitness Wearables
        long fitnessCount = deviceRepository.countByCategory("Fitness Wearables");

        // Fetch count for Smart Home
        long smartHomeCount = deviceRepository.countByCategory("Smart Home");

        // Manually build the fixed response
        Map<String, Object> fitnessEntry = new HashMap<>();
        fitnessEntry.put("name", "Fitness Wearables");
        fitnessEntry.put("sboms", fitnessCount);
        response.add(fitnessEntry);

        Map<String, Object> smartHomeEntry = new HashMap<>();
        smartHomeEntry.put("name", "Smart Home");
        smartHomeEntry.put("sboms", smartHomeCount);
        response.add(smartHomeEntry);

        return response;
    }
}

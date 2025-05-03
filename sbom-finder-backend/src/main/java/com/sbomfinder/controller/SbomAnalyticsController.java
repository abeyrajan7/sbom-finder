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
import com.sbomfinder.model.Supplier;
import com.sbomfinder.repository.SupplierRepository;

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

    @Autowired
    private SupplierRepository supplierRepository;

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

    @GetMapping("/manufacturers")
    public ResponseEntity<List<Map<String, Object>>> getManufacturers() {
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
        List<SoftwarePackage> packages = softwarePackageRepository.findAll();

        Map<String, Long> supplierVulnCounts = new HashMap<>();

        for (SoftwarePackage pkg : packages) {
            String supplierName = Optional.ofNullable(pkg.getSupplier())
                    .map(s -> s.getName())
                    .orElse("Unknown");

            long vulnCount = pkg.getVulnerabilities().size();

            supplierVulnCounts.put(supplierName,
                    supplierVulnCounts.getOrDefault(supplierName, 0L) + vulnCount);
        }

        List<Map<String, Object>> response = supplierVulnCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("vulns", entry.getValue());
                    return map;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("vulns"), (Long) a.get("vulns")))
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

    @GetMapping("/suppliers")
    public ResponseEntity<List<Map<String, Object>>> getSupplierStatistics() {
        List<Supplier> suppliers = supplierRepository.findAll();

        List<Map<String, Object>> result = suppliers.stream().map(supplier -> {
            Map<String, Object> map = new HashMap<>();
            map.put("supplier", supplier.getName());

            List<SoftwarePackage> packages = softwarePackageRepository.findBySupplier(supplier);
            map.put("packageCount", packages.size());

            List<Map<String, String>> packageList = packages.stream().map(pkg -> {
                Map<String, String> pkgMap = new HashMap<>();
                pkgMap.put("name", pkg.getName());
                pkgMap.put("version", pkg.getVersion());
                return pkgMap;
            }).collect(Collectors.toList());

            map.put("packages", packageList);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}

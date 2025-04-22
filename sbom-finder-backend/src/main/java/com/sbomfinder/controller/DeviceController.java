package com.sbomfinder.controller;

import com.sbomfinder.dto.*;
import com.sbomfinder.model.Device;
import com.sbomfinder.repository.DeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.model.ExternalReference;
import com.sbomfinder.model.Vulnerability;
import com.sbomfinder.repository.SoftwarePackageRepository;
import com.sbomfinder.repository.ExternalReferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.stream.Collectors;

import com.sbomfinder.dto.SoftwarePackageDTO;
import com.sbomfinder.dto.ExternalReferenceDTO;
import com.sbomfinder.dto.DeviceDetailsDTO;
import com.sbomfinder.dto.VulnerabilityDTO;
import com.sbomfinder.service.DeviceService;
import com.sbomfinder.service.VulnerabilityService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

import java.util.Optional;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000") 
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private VulnerabilityService vulnerabilityService;

    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    private final DeviceRepository deviceRepository;

    public DeviceController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
    // Fetch Device Details by ID
    @GetMapping("/{deviceId}/details")
    public ResponseEntity<?> getDeviceDetails(@PathVariable Long deviceId) {
        Optional<Device> optionalDevice = deviceRepository.findById(deviceId);

        if (optionalDevice.isPresent()) {
            Device device = optionalDevice.get();

            // 1. Fetch Software Packages
            List<SoftwarePackage> softwarePackages = softwarePackageRepository.findByDeviceId(device.getId());
            System.out.println("Count of packages: " + softwarePackages.size());
            List<SoftwarePackageDTO> softwarePackageDTOs = softwarePackages.stream().map(pkg -> {
                List<VulnerabilityDTO> vulns = vulnerabilityService.getVulnerabilitiesByPackageId(pkg.getId());
                System.out.println("Checking package: " + pkg.getName() + " ID: " + pkg.getId());
                System.out.println("Vulns found: " + vulns.size());
                return new SoftwarePackageDTO(
                        pkg.getName(),
                        pkg.getVersion(),
                        pkg.getSupplier(),
                        pkg.getComponentType(),
                        vulns
                );
            }).collect(Collectors.toList());

            // 2. Fetch External References
            List<ExternalReferenceDTO> externalReferenceDTOs = externalReferenceRepository
                    .findByDeviceNameAndManufacturer(device.getDeviceName(), device.getManufacturer())
                    .stream()
                    .map(ref -> new ExternalReferenceDTO(
                            ref.getReferenceCategory(),
                            ref.getReferenceType(),
                            ref.getReferenceLocator()
                    ))
                    .collect(Collectors.toList());

            // 3. Collect All Vulnerabilities for Packages Linked to This Device
            List<VulnerabilityDTO> vulnDTOs = deviceService.getVulnerabilitiesForDevice(device);

            // 4. Construct DeviceDetailsDTO with Vulns
            DeviceDetailsDTO deviceDetails = new DeviceDetailsDTO(
                    device.getDeviceName(),
                    device.getManufacturer(),
                    device.getCategory(),
                    device.getOperatingSystem(),
                    device.getOsVersion(),
                    device.getKernelVersion(),
                    device.getDigitalFootprint(),
                    device.getSbom().getId(),
                    device.getId(),
                    softwarePackageDTOs,
                    externalReferenceDTOs,
                    vulnDTOs
            );

            return ResponseEntity.ok(deviceDetails);
        } else {
            return ResponseEntity.status(404).body("Device not found");
        }
    }

    // Fetch All Devices
     @GetMapping("/all")
     public ResponseEntity<List<DeviceDetailsDTO>> getAllDevices() {
         List<Device> devices = deviceRepository.findAll();

         List<DeviceDetailsDTO> deviceDetailsList = devices.stream().map(device -> {
             // Map SoftwarePackage entity to SoftwarePackageDTO
             List<SoftwarePackageDTO> softwarePackageDTOs = softwarePackageRepository
                     .findByDeviceNameAndManufacturer(device.getDeviceName(), device.getManufacturer())
                     .stream()
                     .map(pkg -> {
                         List<VulnerabilityDTO> vulnDTOs = pkg.getVulnerabilities().stream().map(v -> {
                             VulnerabilityDTO dto = new VulnerabilityDTO();
                             dto.setCveId(v.getCveId());
                             dto.setDescription(v.getDescription());
                             dto.setSeverity(v.getSeverity());
                             dto.setSourceUrl(v.getSourceUrl());
                             dto.setSeverityLevel(dto.getSeverityLevel()); // if you're computing it during DB insert
                             return dto;
                         }).collect(Collectors.toList());

                         return new SoftwarePackageDTO(
                                 pkg.getName(),
                                 pkg.getVersion(),
                                 pkg.getSupplier(),
                                 pkg.getComponentType(),
                                 vulnDTOs
                         );
                     })
                     .collect(Collectors.toList());

             // Map ExternalReference entity to ExternalReferenceDTO
             List<ExternalReferenceDTO> externalReferenceDTOs = externalReferenceRepository
                     .findByDeviceNameAndManufacturer(device.getDeviceName(), device.getManufacturer())
                     .stream()
                     .map(ref -> new ExternalReferenceDTO(
                             ref.getReferenceCategory(),
                             ref.getReferenceType(),
                             ref.getReferenceLocator()
                     ))
                     .collect(Collectors.toList());
             List<VulnerabilityDTO> vulnDTOs = deviceService.getVulnerabilitiesForDevice(device);
             return new DeviceDetailsDTO(
                     device.getDeviceName(),
                     device.getManufacturer(),
                     device.getCategory(),
                     device.getOperatingSystem(),
                     device.getOsVersion(),
                     device.getKernelVersion(),
                     device.getDigitalFootprint(),
                     (device.getSbom() != null ? device.getSbom().getId() : null),
                     device.getId(),
                     softwarePackageDTOs,
                     externalReferenceDTOs,
                     vulnDTOs
             );
         }).collect(Collectors.toList());

         return ResponseEntity.ok(deviceDetailsList);
     }

    // Compare Two Devices
    @GetMapping("/compare")
    public ResponseEntity<?> compareDevices(@RequestParam Long device1Id, @RequestParam Long device2Id) {
        Optional<Device> optionalDevice1 = deviceRepository.findById(device1Id);
        Optional<Device> optionalDevice2 = deviceRepository.findById(device2Id);

        if (optionalDevice1.isEmpty() || optionalDevice2.isEmpty()) {
            return ResponseEntity.status(404).body("One or both devices not found");
        }

        Device device1 = optionalDevice1.get();
        Device device2 = optionalDevice2.get();

        // Map device 1's packages to DTOs with their vulnerabilities
            List<SoftwarePackage> softwarePackages1 = softwarePackageRepository.findByDeviceId(device1.getId());
            System.out.println("Count of packages: " + softwarePackages1.size());
            List<SoftwarePackageDTO> softwarePackageDTOs1 = softwarePackages1.stream().map(pkg -> {
                List<VulnerabilityDTO> vulns = vulnerabilityService.getVulnerabilitiesByPackageId(pkg.getId());
                System.out.println("Checking package: " + pkg.getName() + " ID: " + pkg.getId());
                System.out.println("Vulns found: " + vulns.size());
                return new SoftwarePackageDTO(
                        pkg.getName(),
                        pkg.getVersion(),
                        pkg.getSupplier(),
                        pkg.getComponentType(),
                        vulns
                );
            }).collect(Collectors.toList());

        // Device 2 packages with vulnerabilities
            List<SoftwarePackage> softwarePackages2 = softwarePackageRepository.findByDeviceId(device2.getId());
            System.out.println("Count of packages: " + softwarePackages2.size());
            List<SoftwarePackageDTO> softwarePackageDTOs2 = softwarePackages2.stream().map(pkg -> {
                List<VulnerabilityDTO> vulns = vulnerabilityService.getVulnerabilitiesByPackageId(pkg.getId());
                System.out.println("Checking package: " + pkg.getName() + " ID: " + pkg.getId());
                System.out.println("Vulns found: " + vulns.size());
                return new SoftwarePackageDTO(
                        pkg.getName(),
                        pkg.getVersion(),
                        pkg.getSupplier(),
                        pkg.getComponentType(),
                        vulns
                );
            }).collect(Collectors.toList());

        // External references
        List<ExternalReferenceDTO> externalRefs1 = device1.getSbom().getExternalReferences().stream()
                .map(ref -> new ExternalReferenceDTO(ref.getReferenceCategory(), ref.getReferenceType(), ref.getReferenceLocator()))
                .collect(Collectors.toList());

        List<ExternalReferenceDTO> externalRefs2 = device2.getSbom().getExternalReferences().stream()
                .map(ref -> new ExternalReferenceDTO(ref.getReferenceCategory(), ref.getReferenceType(), ref.getReferenceLocator()))
                .collect(Collectors.toList());

        // Device details
        DeviceDetailsDTO device1Details = new DeviceDetailsDTO(
                device1.getDeviceName(),
                device1.getManufacturer(),
                device1.getCategory(),
                device1.getOperatingSystem(),
                device1.getOsVersion(),
                device1.getKernelVersion(),
                device1.getDigitalFootprint(),
                device1.getSbom().getId(),
                device1.getId(),
                softwarePackageDTOs1,
                externalRefs1,
                new ArrayList<>()
        );

        DeviceDetailsDTO device2Details = new DeviceDetailsDTO(
                device2.getDeviceName(),
                device2.getManufacturer(),
                device2.getCategory(),
                device2.getOperatingSystem(),
                device2.getOsVersion(),
                device2.getKernelVersion(),
                device2.getDigitalFootprint(),
                device2.getSbom().getId(),
                device2.getId(),
                softwarePackageDTOs2,
                externalRefs2,
                new ArrayList<>()
        );

        // Wrap into comparison
        DeviceComparisonDTO comparisonDTO = new DeviceComparisonDTO(device1Details, device2Details);
        return ResponseEntity.ok(comparisonDTO);
    }

    //device id and name
    @GetMapping ("/list")
    public ResponseEntity<List<Map<String, Object>>> listAllDevices() {
            List<Device> devices = deviceRepository.findAll();
            List<Map<String, Object>> result = devices.stream().map(device -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", device.getId()); // or getId() depending on your field
            map.put("name", device.getDeviceName());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    //search api
    @GetMapping("/search")
    public ResponseEntity<List<DeviceDetailsDTO>> searchDevices(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String operatingSystem) {

        List<Device> devices = deviceRepository.searchWithFilters(query, manufacturer, operatingSystem);

        List<DeviceDetailsDTO> deviceDetailsList = devices.stream().map(device -> {
            List<SoftwarePackageDTO> softwarePackageDTOs = softwarePackageRepository
                    .findByDeviceNameAndManufacturer(device.getDeviceName(), device.getManufacturer())
                    .stream()
                    .map(pkg -> {
                        List<VulnerabilityDTO> vulnDTOs = pkg.getVulnerabilities().stream().map(v -> {
                            VulnerabilityDTO dto = new VulnerabilityDTO();
                            dto.setCveId(v.getCveId());
                            dto.setDescription(v.getDescription());
                            dto.setSeverity(v.getSeverity());
                            dto.setSourceUrl(v.getSourceUrl());
                            dto.setSeverityLevel(v.getSeverityLevel());
                            return dto;
                        }).collect(Collectors.toList());

                        return new SoftwarePackageDTO(
                                pkg.getName(),
                                pkg.getVersion(),
                                pkg.getSupplier(),
                                pkg.getComponentType(),
                                vulnDTOs
                        );
                    })
                    .collect(Collectors.toList());

            List<ExternalReferenceDTO> externalReferenceDTOs = externalReferenceRepository
                    .findByDeviceNameAndManufacturer(device.getDeviceName(), device.getManufacturer())
                    .stream()
                    .map(ref -> new ExternalReferenceDTO(
                            ref.getReferenceCategory(),
                            ref.getReferenceType(),
                            ref.getReferenceLocator()
                    ))
                    .collect(Collectors.toList());

            List<VulnerabilityDTO> vulnDTOs = deviceService.getVulnerabilitiesForDevice(device);

            return new DeviceDetailsDTO(
                    device.getDeviceName(),
                    device.getManufacturer(),
                    device.getCategory(),
                    device.getOperatingSystem(),
                    device.getOsVersion(),
                    device.getKernelVersion(),
                    device.getDigitalFootprint(),
                    device.getSbom().getId(),
                    device.getId(),
                    softwarePackageDTOs,
                    externalReferenceDTOs,
                    vulnDTOs
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(deviceDetailsList);
    }

    //download the sbom of device
    @GetMapping("/download/{deviceId}")
    public ResponseEntity<Resource> downloadSbom(
            @PathVariable Long deviceId,
            @RequestParam(name = "format", defaultValue = "cyclonedx") String format
    ) {
        try {
            Optional<Device> optionalDevice = deviceRepository.findById(deviceId);

            if (optionalDevice.isEmpty()) {
                return ResponseEntity.status(404).body(null);
            }

            Device device = optionalDevice.get();

            List<SoftwarePackage> softwarePackages = softwarePackageRepository.findByDeviceId(device.getId());

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonContent;

            if (format.equalsIgnoreCase("cyclonedx")) {
                // Build CycloneDX format
                Map<String, Object> sbom = new LinkedHashMap<>();
                sbom.put("bomFormat", "CycloneDX");
                sbom.put("specVersion", "1.4");
                sbom.put("version", 1);

                // Components section
                List<Map<String, Object>> components = softwarePackages.stream()
                        .map(pkg -> {
                            Map<String, Object> component = new LinkedHashMap<>();
                            component.put("type", "library");
                            component.put("name", pkg.getName());
                            component.put("version", pkg.getVersion());
                            component.put("purl", pkg.getPurl() != null ? pkg.getPurl() : "NOASSERTION");
                            return component;
                        })
                        .collect(Collectors.toList());

                sbom.put("components", components);

                // Vulnerabilities section
                List<Map<String, Object>> vulnerabilities = new ArrayList<>();

                for (SoftwarePackage pkg : softwarePackages) {
                    if (pkg.getVulnerabilities() != null && !pkg.getVulnerabilities().isEmpty()) {
                        for (Vulnerability vuln : pkg.getVulnerabilities()) {
                            Map<String, Object> vulnMap = new LinkedHashMap<>();
                            vulnMap.put("id", vuln.getCveId());
                            vulnMap.put("source", Map.of("name", "NVD"));

                            Map<String, Object> rating = new LinkedHashMap<>();
                            rating.put("score", vuln.getCvssScore() != null ? vuln.getCvssScore() : 0.0);
                            rating.put("severity", vuln.getSeverity() != null ? vuln.getSeverity() : "UNKNOWN");
                            rating.put("method", "CVSSv3");
                            rating.put("vector", "NOASSERTION");
                            vulnMap.put("ratings", List.of(rating));
                            vulnMap.put("affects", List.of(
                                    Map.of("ref", pkg.getPurl() != null ? pkg.getPurl() : pkg.getName())
                            ));

                            vulnerabilities.add(vulnMap);
                        }
                    }
                }

                if (!vulnerabilities.isEmpty()) {
                    sbom.put("vulnerabilities", vulnerabilities);
                }

                jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sbom);

            } else if (format.equalsIgnoreCase("spdx")) {
                // Build SPDX format (no vulnerabilities)
                Map<String, Object> sbom = new LinkedHashMap<>();
                sbom.put("spdxVersion", "SPDX-2.2");
                sbom.put("SPDXID", "SPDXRef-DOCUMENT");
                sbom.put("name", device.getDeviceName() != null ? device.getDeviceName() : "UnknownDevice");

                List<Map<String, Object>> packages = softwarePackages.stream()
                        .map(pkg -> {
                            Map<String, Object> spdxPackage = new LinkedHashMap<>();
                            spdxPackage.put("SPDXID", "SPDXRef-Package-" + pkg.getName().replaceAll("[^a-zA-Z0-9]", ""));
                            spdxPackage.put("name", pkg.getName());
                            spdxPackage.put("versionInfo", pkg.getVersion() != null ? pkg.getVersion() : "NOASSERTION");
                            spdxPackage.put("downloadLocation", "NOASSERTION");
                            return spdxPackage;
                        })
                        .collect(Collectors.toList());

                sbom.put("packages", packages);

                jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sbom);

            } else {
                return ResponseEntity.badRequest().body(null);
            }

            // Create downloadable file
            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(jsonBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sbom_device_" + deviceId + ".json")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(jsonBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }




}

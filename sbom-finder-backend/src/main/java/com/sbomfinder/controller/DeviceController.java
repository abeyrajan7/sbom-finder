package com.sbomfinder.controller;

import com.sbomfinder.dto.*;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.SbomArchive;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.SbomArchiveRepository;
import org.springframework.http.HttpStatus;
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
import com.sbomfinder.service.SbomArchiveService;
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
    private SbomArchiveRepository sbomArchiveRepository;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SbomArchiveService sbomArchiveService;

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
            List<SoftwarePackageDTO> softwarePackageDTOs = softwarePackages.stream().map(pkg -> {
                List<VulnerabilityDTO> vulns = vulnerabilityService.getVulnerabilitiesByPackageId(pkg.getId());
                String supplierName = pkg.getSupplier() != null ? pkg.getSupplier().getName() : "Unknown Supplier";
                return new SoftwarePackageDTO(
                        pkg.getName(),
                        pkg.getVersion(),
                        supplierName,
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
                         String supplierName = pkg.getSupplier() != null ? pkg.getSupplier().getName() : "Unknown Supplier";


                         return new SoftwarePackageDTO(
                                 pkg.getName(),
                                 pkg.getVersion(),
                                 supplierName,
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
            List<SoftwarePackageDTO> softwarePackageDTOs1 = softwarePackages1.stream().map(pkg -> {
                List<VulnerabilityDTO> vulns = vulnerabilityService.getVulnerabilitiesByPackageId(pkg.getId());
                String supplierName = pkg.getSupplier() != null ? pkg.getSupplier().getName() : "Unknown Supplier";
                return new SoftwarePackageDTO(
                        pkg.getName(),
                        pkg.getVersion(),
                        supplierName,
                        pkg.getComponentType(),
                        vulns
                );
            }).collect(Collectors.toList());

        // Device 2 packages with vulnerabilities
            List<SoftwarePackage> softwarePackages2 = softwarePackageRepository.findByDeviceId(device2.getId());
            List<SoftwarePackageDTO> softwarePackageDTOs2 = softwarePackages2.stream().map(pkg -> {
                List<VulnerabilityDTO> vulns = vulnerabilityService.getVulnerabilitiesByPackageId(pkg.getId());
                String supplierName = pkg.getSupplier() != null ? pkg.getSupplier().getName() : "Unknown Supplier";
                return new SoftwarePackageDTO(
                        pkg.getName(),
                        pkg.getVersion(),
                        supplierName,
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

    //List all details
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
            @RequestParam(required = false) String operatingSystem,
            @RequestParam(required = false) String category) {

        List<Device> devices = deviceRepository.searchWithFuzzyFilters(query, manufacturer, operatingSystem, category);

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
                        String supplierName = pkg.getSupplier() != null ? pkg.getSupplier().getName() : "Unknown Supplier";

                        return new SoftwarePackageDTO(
                                pkg.getName(),
                                pkg.getVersion(),
                                supplierName,
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



// download the sbom of device
@GetMapping("/download/{deviceId}")
public ResponseEntity<Resource> downloadArchivedSbom(
        @PathVariable Long deviceId,
        @RequestParam(name = "format", defaultValue = "cyclonedx") String format
) {
    try {
        Optional<SbomArchive> optionalArchive = sbomArchiveRepository.findTopByDeviceIdAndIsLatestTrue(deviceId);
        if (optionalArchive.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        SbomArchive archive = optionalArchive.get();
        String unifiedJson = archive.getSbomContent();

        ObjectMapper objectMapper = new ObjectMapper();
        UnifiedSbomData sbomData = objectMapper.readValue(unifiedJson, UnifiedSbomData.class);

        String outputJson;

        if (format.equalsIgnoreCase("cyclonedx")) {
            outputJson = sbomArchiveService.generateCycloneDxJson(sbomData);
        } else if (format.equalsIgnoreCase("spdx")) {
            outputJson = sbomArchiveService.generateSpdxJson(sbomData);
        } else {
            return ResponseEntity.badRequest().body(null);
        }

        byte[] jsonBytes = outputJson.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(jsonBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sbom_device_" + deviceId + "." + format.toLowerCase() + ".json")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(jsonBytes.length)
                .body(resource);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}

    // get all the archived SBOMs
    @GetMapping("/archives/all")
    public ResponseEntity<?> getAllDeviceArchives() {
        List<Device> allDevices = deviceRepository.findAll();

        List<Map<String, Object>> deviceArchiveList = allDevices.stream()
                .map(device -> {
                    List<SbomArchive> archives = sbomArchiveRepository.findAllByDeviceId(device.getId());
                    if (archives.isEmpty()) return null;  // Skip devices with no archives

                    List<Map<String, Object>> archiveList = archives.stream().map(archive -> {
                        Map<String, Object> archiveMap = new HashMap<>();
                        archiveMap.put("name", "Version - " + archive.getVersion());
                        archiveMap.put("archiveId", archive.getId());
                        archiveMap.put("isLatest", archive.getIsLatest());
                        return archiveMap;
                    }).collect(Collectors.toList());

                    Map<String, Object> deviceMap = new HashMap<>();
                    deviceMap.put("deviceName", device.getDeviceName());
                    deviceMap.put("archives", archiveList);
                    return deviceMap;
                })
                .filter(Objects::nonNull) // Only include devices with archives
                .collect(Collectors.toList());

        return ResponseEntity.ok(deviceArchiveList);
    }

    //Download from the archoved sboms
    @GetMapping("/download/archive/{archiveId}")
    public ResponseEntity<Resource> downloadArchivedSbomById(
            @PathVariable Long archiveId,
            @RequestParam(name = "format", defaultValue = "cyclonedx") String format
    ) {
        try {
            Optional<SbomArchive> optionalArchive = sbomArchiveRepository.findById(archiveId);
            if (optionalArchive.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            SbomArchive archive = optionalArchive.get();
            String unifiedJson = archive.getSbomContent();

            ObjectMapper objectMapper = new ObjectMapper();
            UnifiedSbomData sbomData = objectMapper.readValue(unifiedJson, UnifiedSbomData.class);

            String outputJson;

            if (format.equalsIgnoreCase("cyclonedx")) {
                outputJson = sbomArchiveService.generateCycloneDxJson(sbomData);
            } else if (format.equalsIgnoreCase("spdx")) {
                outputJson = sbomArchiveService.generateSpdxJson(sbomData);
            } else {
                return ResponseEntity.badRequest().body(null);
            }

            byte[] jsonBytes = outputJson.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(jsonBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sbom_archive_" + archiveId + "." + format.toLowerCase() + ".json")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(jsonBytes.length)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // download the latest version's sbom
    @GetMapping("/download/{deviceId}/latest")
    public ResponseEntity<Resource> downloadLatestSbom(
            @PathVariable Long deviceId,
            @RequestParam(name="format", defaultValue = "cyclonedx") String format
    )
    {
        try {
            Optional<SbomArchive> optionalArchive = sbomArchiveRepository.findTopByDeviceIdAndIsLatestTrue(deviceId);
            if (optionalArchive.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String unifiedJson = optionalArchive.get().getSbomContent();
            ObjectMapper objectMapper = new ObjectMapper();
            UnifiedSbomData sbomData = objectMapper.readValue(unifiedJson, UnifiedSbomData.class);

            String outputJson;
            if (format.equalsIgnoreCase("cyclonedx")) {
                outputJson = sbomArchiveService.generateCycloneDxJson(sbomData);
            } else if (format.equalsIgnoreCase("spdx")) {
                outputJson = sbomArchiveService.generateSpdxJson(sbomData);
            } else {
                return ResponseEntity.badRequest().body(null);
            }

            byte[] jsonBytes = outputJson.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(jsonBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=latest_sbom_device_" + deviceId + "." + format + ".json")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(jsonBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
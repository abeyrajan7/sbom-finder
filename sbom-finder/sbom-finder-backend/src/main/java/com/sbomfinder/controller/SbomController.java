package com.sbomfinder.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.ExternalReference;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.repository.DeviceRepository;
import com.sbomfinder.repository.ExternalReferenceRepository;
import com.sbomfinder.repository.SbomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import com.sbomfinder.dto.DeviceDetailsDTO;
import com.sbomfinder.dto.ExternalReferenceDTO;
import com.sbomfinder.dto.SoftwarePackageDTO;
import java.io.IOException;
import java.time.OffsetDateTime;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.repository.SoftwarePackageRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.sbomfinder.dto.CompareDevicesRequest;
import com.sbomfinder.dto.DeviceComparisonDTO;
import com.sbomfinder.dto.NormalizedSbomDataDTO;
import com.sbomfinder.model.Device;
import com.sbomfinder.model.SoftwarePackage;
import com.sbomfinder.repository.DeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sbomfinder.service.SbomService;

@RestController
@CrossOrigin(origins = "http://localhost:3000") 
@RequestMapping("/api/sboms")
public class SbomController {

    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;

    @Autowired
    private SbomRepository sbomRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ObjectMapper objectMapper; // Jackson JSON Mapper

    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    @Autowired
    private SbomService sbomService;

    @PostMapping("/upload-sbom")
public ResponseEntity<String> uploadSbom(@RequestParam("sbomFile") MultipartFile sbomFile,
                                         @RequestParam("category") String category) {
    try {
        JsonNode jsonNode = objectMapper.readTree(sbomFile.getInputStream());
        String format = detectFormat(jsonNode);

        NormalizedSbomDataDTO sbomData;
        switch (format) {
            case "cyclonedx":
                sbomData = parseCycloneDX(jsonNode, category);
                break;
            case "spdx":
                sbomData = parseSPDX(jsonNode, category);
                break;
            default:
                return ResponseEntity.badRequest().body("Unsupported SBOM format");
        }
        Sbom newSbom = new Sbom(sbomData.getFormat(), sbomData.getSpecVersion(), sbomData.getDataLicense(),
                sbomData.getDocumentNamespace(), sbomData.getCreatedTime(),
                sbomData.getVendor(), sbomData.getToolName());
        sbomRepository.save(newSbom);

        // Save External References
        for (JsonNode ref : sbomData.getExternalReferences()) {
            ExternalReference extRef = new ExternalReference(newSbom,
                    ref.path("referenceCategory").asText(""),
                    ref.path("referenceType").asText(""),
                    ref.path("referenceLocator").asText(""));
            externalReferenceRepository.save(extRef);
        }

        // Save Device
        Device device = deviceRepository.findByDeviceNameAndManufacturer(
            sbomData.getDeviceName(),
            sbomData.getManufacturer()
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
        if (format.equals("spdx")) {
            sbomService.processSpdxPackages(sbomData.getPackages(), newSbom, device);
        } else if (format.equals("cyclonedx")) {
            sbomService.processCycloneDXPackages(sbomData.getPackages(), newSbom, device);
        }

        return ResponseEntity.ok("SBOM parsed and stored successfully for format: " + format);
    } catch (IOException e) {
        return ResponseEntity.badRequest().body("Error parsing SBOM: " + e.getMessage());
    }
}

    // DTO for API Response
    public static class DeviceDetailsResponse {
        public String deviceName;
        public String manufacturer;
        public String category;
        public String operatingSystem;
        public Sbom sbomDetails;
        public List<SoftwarePackage> softwarePackages;
        public List<ExternalReference> externalReferences;

        public DeviceDetailsResponse(Device device, Sbom sbom, List<SoftwarePackage> softwarePackages, List<ExternalReference> externalReferences) {
            this.deviceName = device.getDeviceName();
            this.manufacturer = device.getManufacturer();
            this.category = device.getCategory();
            this.operatingSystem = device.getOperatingSystem();
            this.sbomDetails = sbom;
            this.softwarePackages = softwarePackages;
            this.externalReferences = externalReferences;
        }
    }

    private String detectFormat(JsonNode jsonNode) {
        if (jsonNode.has("bomFormat") && "CycloneDX".equalsIgnoreCase(jsonNode.get("bomFormat").asText())) {
            return "cyclonedx";
        } else if (jsonNode.has("spdxVersion")) {
            return "spdx";
        }
        return "unknown";
    }

    // function to parse SPDX format sbom
    private NormalizedSbomDataDTO parseCycloneDX(JsonNode jsonNode, String category) {
        NormalizedSbomDataDTO data = new NormalizedSbomDataDTO();
        data.setFormat("CycloneDX");
        data.setSpecVersion(jsonNode.path("specVersion").asText(""));
        data.setDataLicense(jsonNode.path("dataLicense").asText(""));
        data.setDocumentNamespace(jsonNode.path("documentNamespace").asText(""));
        JsonNode metadataNode = jsonNode.path("metadata");

        // Safely parse created time
        if (metadataNode.has("timestamp")) {
           String timestamp = metadataNode.path("timestamp").asText();
          if (timestamp != null && !timestamp.isEmpty()) {
              data.setCreatedTime(java.time.OffsetDateTime.parse(timestamp).toLocalDateTime());
          }
        }

        // Safe access for tools[0]
        JsonNode toolsArray = metadataNode.path("tools");
        if (toolsArray != null && toolsArray.isArray() && toolsArray.size() > 0) {
            JsonNode toolNode = toolsArray.get(0);
            data.setVendor(toolNode.path("vendor").asText("Unknown Vendor"));
            data.setToolName(toolNode.path("name").asText("Unknown Tool"));
        } else {
            data.setVendor("Unknown Vendor");
            data.setToolName("Unknown Tool");
        }
        // Parse component info
        JsonNode component = metadataNode.path("component");
        String rawName = component.path("name").asText("Unknown Device");
        System.out.println(SbomService.cleanDeviceName(rawName));
        data.setDeviceName(SbomService.cleanDeviceName(rawName));
        data.setManufacturer(component.path("manufacturer").asText("Unknown Manufacturer"));
        data.setOperatingSystem(component.path("operatingSystem").asText("Unknown OS"));
        data.setOsVersion(component.path("version").asText("Unknown Version"));
        data.setKernelVersion(component.path("kernel").asText("Unknown Kernel"));

        // Digital Footprint
        Set<String> footprintSet = new HashSet<>();

        // 1. From external references
        JsonNode externalRefs = jsonNode.path("externalReferences");
        if (externalRefs.isArray()) {
            for (JsonNode ref : externalRefs) {
                String locator = ref.path("referenceLocator").asText();
                String domain = SbomService.extractDomain(locator);
                if (!domain.isEmpty()) footprintSet.add(domain);
            }
        }

        // 2. From components
        JsonNode components = jsonNode.path("components");
            if (components.isArray()) {
                for (JsonNode comp : components) {
                // downloadLocation
                String downloadUrl = comp.path("downloadLocation").asText();
                String domain = SbomService.extractDomain(downloadUrl);
                if (!domain.isEmpty()) footprintSet.add(domain);

                // externalRefs
                JsonNode extRefs = comp.path("externalRefs");
                if (extRefs.isArray()) {
                    for (JsonNode ref : extRefs) {
                        String locator = ref.path("referenceLocator").asText();
                        if (locator.startsWith("pkg:")) {
                            String[] parts = locator.split("/");
                            if (parts.length > 1) footprintSet.add(parts[0].replace("pkg:", ""));
                        } else {
                            String urlDomain = SbomService.extractDomain(locator);
                            if (!urlDomain.isEmpty()) footprintSet.add(urlDomain);
                        }
                    }
                }
            }
        }

        // 3. From tools
        JsonNode toolsArrayMetadata = jsonNode.path("metadata").path("tools");
        if (toolsArrayMetadata.isArray()) {
            for (JsonNode tool : toolsArrayMetadata) {
                String toolName = tool.path("name").asText(null);
                if (toolName != null && !toolName.isEmpty()) footprintSet.add(toolName);
                String vendor = tool.path("vendor").asText(null);
                if (vendor != null && !vendor.isEmpty()) footprintSet.add(vendor);
            }
        }

        // Final fallback
        if (footprintSet.isEmpty()) {
            data.setDigitalFootprint("Not Available");
        } else {
            String joined = String.join(", ", footprintSet);
            data.setDigitalFootprint(joined);
        }

        // Packages
        List<JsonNode> componentsList = new ArrayList<>();
        JsonNode componentsNode = jsonNode.path("components");
        if (componentsNode.isArray()) {
            componentsList = StreamSupport.stream(componentsNode.spliterator(), false)
                    .collect(Collectors.toList());
        }
        data.setPackages(componentsList);

        // External References
        List<JsonNode> externalRefsList = new ArrayList<>();
        JsonNode extRefsNode = jsonNode.path("externalReferences");
        if (extRefsNode.isArray()) {
            externalRefsList = StreamSupport.stream(extRefsNode.spliterator(), false)
                    .collect(Collectors.toList());
        }
        data.setExternalReferences(externalRefsList);

        data.setCategory(category);
        return data;
    }

    // function to parse SPDX format sbom
    private NormalizedSbomDataDTO parseSPDX(JsonNode jsonNode, String category) {
            NormalizedSbomDataDTO data = new NormalizedSbomDataDTO();
            data.setFormat("SPDX");
            data.setSpecVersion(jsonNode.path("spdxVersion").asText());
            data.setDataLicense(jsonNode.path("dataLicense").asText());
            data.setDocumentNamespace(jsonNode.path("documentNamespace").asText());
            data.setCreatedTime(LocalDateTime.now()); // SPDX doesn't always provide timestamp

            JsonNode creationInfo = jsonNode.path("creationInfo");
            if (creationInfo.has("creators") && creationInfo.path("creators").isArray()) {
                data.setVendor(creationInfo.path("creators").get(0).asText("Unknown Vendor"));
            } else {
                data.setVendor("Unknown Vendor");
            }
            data.setToolName(creationInfo.path("comment").asText("Unknown"));
            String rawName = jsonNode.path("name").asText("Unknown Device");
            data.setDeviceName(SbomService.cleanDeviceName(rawName));
            data.setManufacturer("Unknown"); // SPDX often lacks manufacturer info
            data.setOperatingSystem("Unknown OS");
            data.setOsVersion("Unknown Version");
            data.setKernelVersion("Unknown Kernel");
            data.setDigitalFootprint("Not Available");

            // Safe way to extract packages list
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
}

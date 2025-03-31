package com.sbomfinder.dto;

import java.util.List;
import com.sbomfinder.model.Device;

public class DeviceDetailsDTO {
    private Long deviceId;
    private Long sbomId;
    private String name;
    private String manufacturer;
    private String category;  // ✅ Ensure this field exists
    private String operatingSystem;
    private String osVersion;
    private String kernelVersion;
    private String digitalFootprint;
    private List<SoftwarePackageDTO> softwarePackages;
    private List<ExternalReferenceDTO> externalReferences;
    private List<VulnerabilityDTO> vulnerabilities;
    public DeviceDetailsDTO(String name, String manufacturer, String category, String operatingSystem,
                            String osVersion, String kernelVersion, String digitalFootprint, Long sbomId, Long deviceId,
                            List<SoftwarePackageDTO> softwarePackages, List<ExternalReferenceDTO> externalReferences,
                            List<VulnerabilityDTO> vulnerabilities) {
        this.deviceId = deviceId;
        this.sbomId = sbomId;
        this.name = name;
        this.manufacturer = manufacturer;
        this.category = category;
        this.operatingSystem = operatingSystem;
        this.osVersion = osVersion;
        this.kernelVersion = kernelVersion;
        this.digitalFootprint = digitalFootprint;
        this.softwarePackages = softwarePackages;
        this.externalReferences = externalReferences;
        this.vulnerabilities = vulnerabilities;
    }

    // ✅ Getters and Setters
    public String getName() { return name; }
    public String getManufacturer() { return manufacturer; }
    public String getCategory() { return category; }
    public String getOperatingSystem() { return operatingSystem; }
    public String getOsVersion() { return osVersion; }
    public String getKernelVersion() { return kernelVersion; }
    public String getDigitalFootprint() { return digitalFootprint; }
    public List<SoftwarePackageDTO> getSoftwarePackages() { return softwarePackages; }
    public List<ExternalReferenceDTO> getExternalReferences() { return externalReferences; }
    public Long getSbomId() { return sbomId; }
    public Long getDeviceId() { return deviceId; }
    public List<VulnerabilityDTO> getVulnerabilities() {return vulnerabilities; }
    public void setVulnerabilities(List<VulnerabilityDTO> vulnerabilities) { this.vulnerabilities = vulnerabilities; }
}

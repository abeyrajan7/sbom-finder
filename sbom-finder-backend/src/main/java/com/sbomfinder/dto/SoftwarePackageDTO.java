package com.sbomfinder.dto;
import java.util.List;

public class SoftwarePackageDTO {
    private String name;
    private String version;
    private String supplierName;
    private String componentType;
    private List<VulnerabilityDTO> vulnerabilities;

    public SoftwarePackageDTO(String name, String version, String supplierName, String componentType, List<VulnerabilityDTO> vulnerabilities) {
        this.name = name;
        this.version = version;
        this.supplierName = supplierName;
        this.componentType = componentType;
        this.vulnerabilities = vulnerabilities;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getSupplierName() { return supplierName; }
    public String getComponentType() { return componentType; }
    public List<VulnerabilityDTO> getVulnerabilities() {
        return vulnerabilities;
    }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public void setVulnerabilities(List<VulnerabilityDTO> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
}

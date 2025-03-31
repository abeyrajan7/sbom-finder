package com.sbomfinder.dto;
import java.util.List;

public class SoftwarePackageDTO {
    private String name;
    private String version;
    private String supplier;
    private String componentType;
    private List<VulnerabilityDTO> vulnerabilities;

    public SoftwarePackageDTO(String name, String version, String supplier, String componentType, List<VulnerabilityDTO> vulnerabilities) {
        this.name = name;
        this.version = version;
        this.supplier = supplier;
        this.componentType = componentType;
        this.vulnerabilities = vulnerabilities;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getSupplier() { return supplier; }
    public String getComponentType() { return componentType; }
    public List<VulnerabilityDTO> getVulnerabilities() {
        return vulnerabilities;
    }
    public void setVulnerabilities(List<VulnerabilityDTO> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
}

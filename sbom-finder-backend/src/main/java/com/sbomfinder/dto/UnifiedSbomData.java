package com.sbomfinder.dto;

import java.util.List;

public class UnifiedSbomData {
    private String deviceName;
    private String version;
    private List<UnifiedComponent> components;
    private List<UnifiedVulnerability> vulnerabilities;

    // Getters and Setters
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<UnifiedComponent> getComponents() {
        return components;
    }

    public void setComponents(List<UnifiedComponent> components) {
        this.components = components;
    }

    public List<UnifiedVulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(List<UnifiedVulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
}

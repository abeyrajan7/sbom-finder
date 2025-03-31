package com.sbomfinder.dto;

import org.springframework.web.multipart.MultipartFile;

public class UploadSbomRequest {
    private MultipartFile sbomFile;
    private String deviceName;
    private String manufacturer;
    private String category;
    private String operatingSystem;

    // Getters and Setters
    public MultipartFile getSbomFile() { return sbomFile; }
    public void setSbomFile(MultipartFile sbomFile) { this.sbomFile = sbomFile; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
}

package com.sbomfinder.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public class NormalizedSbomDataDTO {
    private String format;
    private String specVersion;
    private String dataLicense;
    private String documentNamespace;
    private LocalDateTime createdTime;

    private String vendor;
    private String toolName;

    private String deviceName;
    private String manufacturer;
    private String category;
    private String operatingSystem;
    private String osVersion;
    private String kernelVersion;
    private String digitalFootprint;

    private List<JsonNode> packages;
    private List<JsonNode> externalReferences;

//Getters
    public String getFormat() {
        return format;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getDataLicense() {
        return dataLicense;
    }

    public String getDocumentNamespace() {
        return documentNamespace;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public String getVendor() {
        return vendor;
    }

    public String getToolName() {
        return toolName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getCategory() {
        return category;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public String getDigitalFootprint() {
        return digitalFootprint;
    }

    public List<JsonNode> getPackages() {
        return packages;
    }

    public List<JsonNode> getExternalReferences() {
        return externalReferences;
    }

//Setters
    public void setFormat(String format) {
        this.format = format;
    }

    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    public void setDataLicense(String dataLicense) {
        this.dataLicense = dataLicense;
    }

    public void setDocumentNamespace(String documentNamespace) {
        this.documentNamespace = documentNamespace;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public void setDigitalFootprint(String digitalFootprint) {
        this.digitalFootprint = digitalFootprint;
    }

    public void setPackages(List<JsonNode> packages) {
        this.packages = packages;
    }

    public void setExternalReferences(List<JsonNode> externalReferences) {
        this.externalReferences = externalReferences;
    }

}




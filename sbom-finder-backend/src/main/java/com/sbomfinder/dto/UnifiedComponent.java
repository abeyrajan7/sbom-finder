package com.sbomfinder.dto;

public class UnifiedComponent {
    private String name;
    private String version;
    private String purl;
    private String supplier;

    public UnifiedComponent() {}

    public UnifiedComponent(String name, String version, String purl) {
        this.name = name;
        this.version = version;
        this.purl = purl;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
}

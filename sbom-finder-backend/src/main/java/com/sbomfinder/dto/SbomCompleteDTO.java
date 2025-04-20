package com.sbomfinder.dto;

import java.util.List;

public class SbomCompleteDTO {
    private String sbomName;
    private String createdDate;
    private String creatorOrganization;
    private String creatorTool;
    private DeviceDetailsDTO device;
    private List<SoftwarePackageDTO> softwarePackages;

    // Getters and Setters

    public String getSbomName() {
        return sbomName;
    }

    public void setSbomName(String sbomName) {
        this.sbomName = sbomName;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatorOrganization() {
        return creatorOrganization;
    }

    public void setCreatorOrganization(String creatorOrganization) {
        this.creatorOrganization = creatorOrganization;
    }

    public String getCreatorTool() {
        return creatorTool;
    }

    public void setCreatorTool(String creatorTool) {
        this.creatorTool = creatorTool;
    }

    public DeviceDetailsDTO getDevice() {
        return device;
    }

    public void setDevice(DeviceDetailsDTO  device) {
        this.device = device;
    }

    public List<SoftwarePackageDTO> getSoftwarePackages() {
        return softwarePackages;
    }

    public void setSoftwarePackages(List<SoftwarePackageDTO> softwarePackages) {
        this.softwarePackages = softwarePackages;
    }
}

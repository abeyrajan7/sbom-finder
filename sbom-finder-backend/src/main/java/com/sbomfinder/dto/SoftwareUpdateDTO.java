package com.sbomfinder.dto;

public class SoftwareUpdateDTO {
    private String softwareName;
    private String status;
    private String oldVersion;
    private String newVersion;

    public SoftwareUpdateDTO(String softwareName, String status, String oldVersion, String newVersion) {
        this.softwareName = softwareName;
        this.status = status;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public String getSoftwareName() { return softwareName; }
    public String getStatus() { return status; }
    public String getOldVersion() { return oldVersion; }
    public String getNewVersion() { return newVersion; }
}

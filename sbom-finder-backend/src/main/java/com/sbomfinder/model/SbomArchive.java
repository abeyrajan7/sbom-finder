package com.sbomfinder.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;

import com.sbomfinder.model.Device;

@Entity
@Table(name = "sbom_archive")
public class SbomArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "device_id")
    private Device device;

    private String version;

    @Column(name = "sbom_content", columnDefinition = "TEXT")
    private String sbomContent;

    private boolean isLatest;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSbomContent() {
        return sbomContent;
    }

    public void setSbomContent(String sbomContent) {
        this.sbomContent = sbomContent;
    }

    public boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(boolean isLatest) {
        this.isLatest = isLatest;
    }
}

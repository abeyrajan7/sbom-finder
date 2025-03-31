package com.sbomfinder.model;

import jakarta.persistence.*;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "sbom_id", referencedColumnName = "id")
    private Sbom sbom;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "manufacturer", nullable = false)
    private String manufacturer;

    @Column
    private String category; // ✅ User will manually input this

    @Column(name = "operating_system")
    private String operatingSystem;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "kernel_version")
    private String kernelVersion;

    @Column(name = "digital_footprint", length = 500)
    private String digitalFootprint;

    public Device() {}

    public Device(String deviceName, String manufacturer, String category, String operatingSystem, String osVersion,
                  String kernelVersion, String digitalFootprint, Sbom sbom) {
        this.deviceName = deviceName;
        this.manufacturer = manufacturer;
        this.category = category;
        this.operatingSystem = operatingSystem;
        this.osVersion = osVersion;
        this.kernelVersion = kernelVersion;
        this.digitalFootprint = digitalFootprint;
        this.sbom = sbom;
    }

    // ✅ Getters
    public Long getId() { return id; }
    public String getDeviceName() { return deviceName; }
    public String getManufacturer() { return manufacturer; }
    public String getCategory() { return category; }
    public String getOperatingSystem() { return operatingSystem; }
    public String getOsVersion() { return osVersion; }
    public String getKernelVersion() { return kernelVersion; }
    public String getDigitalFootprint() { return digitalFootprint; }
    public Sbom getSbom() { return sbom; }

    // ✅ Setters
    public void setId(Long id) { this.id = id; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public void setCategory(String category) { this.category = category; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public void setKernelVersion(String kernelVersion) { this.kernelVersion = kernelVersion; }
    public void setDigitalFootprint(String digitalFootprint) { this.digitalFootprint = digitalFootprint; }
    public void setSbom(Sbom sbom) { this.sbom = sbom; }
}

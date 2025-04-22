package com.sbomfinder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "sbom_files")
public class Sbom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(unique = true, columnDefinition = "TEXT")
    private String hash;

    @Column(name = "spdx_version", nullable = false, columnDefinition = "TEXT")
    private String spdxVersion;

    @Column(name = "data_license", nullable = false, columnDefinition = "TEXT")
    private String dataLicense;

    @Column(name = "document_namespace", nullable = false, columnDefinition = "TEXT")
    private String documentNamespace;

    @Column(nullable = false)
    private LocalDateTime created;

    @Column(name = "creator_organization", columnDefinition = "TEXT")
    private String creatorOrganization;

    @Column(name = "creator_tool", columnDefinition = "TEXT")
    private String creatorTool;

    @Column(name = "sbom_content", columnDefinition = "TEXT")
    private String sbomContent;

    @OneToMany(mappedBy = "sbom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExternalReference> externalReferences;

    @OneToMany(mappedBy = "sbom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoftwarePackage> softwarePackages;

    @Column(name = "version")
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;


    public Sbom() {}

    public Sbom(String name, String spdxVersion, String dataLicense, String documentNamespace, LocalDateTime created, String creatorOrganization, String creatorTool, String hash) {
        this.name = name;
        this.spdxVersion = spdxVersion;
        this.dataLicense = dataLicense;
        this.documentNamespace = documentNamespace;
        this.created = created;
        this.creatorOrganization = creatorOrganization;
        this.creatorTool = creatorTool;
        this.hash = hash;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSpdxVersion() { return spdxVersion; }
    public String getDataLicense() { return dataLicense; }
    public String getDocumentNamespace() { return documentNamespace; }
    public LocalDateTime getCreated() { return created; }
    public String getCreatorOrganization() { return creatorOrganization; }
    public String getCreatorTool() { return creatorTool; }
    public List<SoftwarePackage> getSoftwarePackages() { return softwarePackages; }
    public List<ExternalReference> getExternalReferences() { return externalReferences; }
    public String getHash() { return hash; }
    public String getSbomContent() { return sbomContent; }
    public String getVersion() { return version; }
    public Device getDevice() { return device; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSpdxVersion(String spdxVersion) { this.spdxVersion = spdxVersion; }
    public void setDataLicense(String dataLicense) { this.dataLicense = dataLicense; }
    public void setDocumentNamespace(String documentNamespace) { this.documentNamespace = documentNamespace; }
    public void setCreated(LocalDateTime created) { this.created = created; }
    public void setCreatorOrganization(String creatorOrganization) { this.creatorOrganization = creatorOrganization; }
    public void setCreatorTool(String creatorTool) { this.creatorTool = creatorTool; }
    public void setSoftwarePackages(List<SoftwarePackage> softwarePackages) { this.softwarePackages = softwarePackages; }
    public void setExternalReferences(List<ExternalReference> externalReferences) { this.externalReferences = externalReferences; }
    public void setHash(String hash) { this.hash = hash; }
    public void setSbomContent(String sbomContent) { this.sbomContent = sbomContent; }
    public void setVersion(String version) { this.version = version; }
    public void setDevice(Device device) { this.device = device; }

}

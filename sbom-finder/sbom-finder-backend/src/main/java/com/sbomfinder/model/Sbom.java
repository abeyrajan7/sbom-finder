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

    @Column(nullable = false)
    private String name;

    @Column(name = "spdx_version", nullable = false)
    private String spdxVersion;

    @Column(name = "data_license", nullable = false)
    private String dataLicense;

    @Column(name = "document_namespace", nullable = false)
    private String documentNamespace;

    @Column(nullable = false)
    private LocalDateTime created;

    @Column(name = "creator_organization")
    private String creatorOrganization;

    @Column(name = "creator_tool")
    private String creatorTool;

    @OneToOne(mappedBy = "sbom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Device device;

    @OneToMany(mappedBy = "sbom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExternalReference> externalReferences;

    @OneToMany(mappedBy = "sbom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoftwarePackage> softwarePackages;

    public Sbom() {}

    public Sbom(String name, String spdxVersion, String dataLicense, String documentNamespace, LocalDateTime created, String creatorOrganization, String creatorTool) {
        this.name = name;
        this.spdxVersion = spdxVersion;
        this.dataLicense = dataLicense;
        this.documentNamespace = documentNamespace;
        this.created = created;
        this.creatorOrganization = creatorOrganization;
        this.creatorTool = creatorTool;
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
}

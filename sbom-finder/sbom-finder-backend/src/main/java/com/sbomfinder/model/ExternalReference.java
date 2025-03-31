package com.sbomfinder.model;

import jakarta.persistence.*;

@Entity
@Table(name = "external_references")
public class ExternalReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sbom_id", nullable = false)
    private Sbom sbom;

    @Column(name = "reference_category", nullable = false)
    private String referenceCategory;

    @Column(name = "reference_type", nullable = false)
    private String referenceType;

    @Column(name = "reference_locator", nullable = false)
    private String referenceLocator;

    // Constructors
    public ExternalReference() {}

    public ExternalReference(Sbom sbom, String referenceCategory, String referenceType, String referenceLocator) {
        this.sbom = sbom;
        this.referenceCategory = referenceCategory;
        this.referenceType = referenceType;
        this.referenceLocator = referenceLocator;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Sbom getSbom() { return sbom; }
    public String getReferenceCategory() { return referenceCategory; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceLocator() { return referenceLocator; }

    public void setId(Long id) { this.id = id; }
    public void setSbom(Sbom sbom) { this.sbom = sbom; }
    public void setReferenceCategory(String referenceCategory) { this.referenceCategory = referenceCategory; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setReferenceLocator(String referenceLocator) { this.referenceLocator = referenceLocator; }
}

package com.sbomfinder.dto;

public class ExternalReferenceDTO {
    private String referenceCategory;
    private String referenceType;
    private String referenceLocator;

    public ExternalReferenceDTO(String referenceCategory, String referenceType, String referenceLocator) {
        this.referenceCategory = referenceCategory;
        this.referenceType = referenceType;
        this.referenceLocator = referenceLocator;
    }

    public String getReferenceCategory() { return referenceCategory; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceLocator() { return referenceLocator; }
}

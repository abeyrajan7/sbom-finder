package com.sbomfinder.dto;

public class CommonSoftwareDTO {
    private String name;
    private String versionDevice1;
    private String versionDevice2;
    private String supplierDevice1;
    private String supplierDevice2;
    private String componentType;

    public CommonSoftwareDTO(String name, String versionDevice1, String versionDevice2,
                             String supplierDevice1, String supplierDevice2, String componentType) {
        this.name = name;
        this.versionDevice1 = versionDevice1;
        this.versionDevice2 = versionDevice2;
        this.supplierDevice1 = supplierDevice1;
        this.supplierDevice2 = supplierDevice2;
        this.componentType = componentType;
    }
    public String getName() { return name; }
    public String getVersionDevice1() { return versionDevice1; }
    public String getVersionDevice2() { return versionDevice2; }
    public String getSupplierDevice1() { return supplierDevice1; }
    public String getSupplierDevice2() { return supplierDevice2; }
    public String getComponentType() { return componentType; }
}

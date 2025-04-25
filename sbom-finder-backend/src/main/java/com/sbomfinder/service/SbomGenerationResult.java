package com.sbomfinder.service;

import com.sbomfinder.model.Device;
import com.sbomfinder.model.Sbom;

public class SbomGenerationResult {
    private final String version;
    private final Device device;
    private Sbom sbom;

    public SbomGenerationResult(String version, Device device) {
        this.version = version;
        this.device = device;
        this.sbom = sbom;
    }


    public Sbom getSbom() {
        return sbom;
    }

    public String getVersion() {
        return version;
    }

    public Device getDevice() {
        return device;
    }
}

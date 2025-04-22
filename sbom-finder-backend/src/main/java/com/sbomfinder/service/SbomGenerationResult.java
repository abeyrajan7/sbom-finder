package com.sbomfinder.service;

import com.sbomfinder.model.Device;

public class SbomGenerationResult {
    private final String version;
    private final Device device;

    public SbomGenerationResult(String version, Device device) {
        this.version = version;
        this.device = device;
    }

    public String getVersion() {
        return version;
    }

    public Device getDevice() {
        return device;
    }
}

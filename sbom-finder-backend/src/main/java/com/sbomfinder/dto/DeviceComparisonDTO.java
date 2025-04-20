package com.sbomfinder.dto;

public class DeviceComparisonDTO {
    private DeviceDetailsDTO device1;
    private DeviceDetailsDTO device2;

    public DeviceComparisonDTO(DeviceDetailsDTO device1, DeviceDetailsDTO device2) {
        this.device1 = device1;
        this.device2 = device2;
    }

    public DeviceDetailsDTO getDevice1() { return device1; }
    public DeviceDetailsDTO getDevice2() { return device2; }
}

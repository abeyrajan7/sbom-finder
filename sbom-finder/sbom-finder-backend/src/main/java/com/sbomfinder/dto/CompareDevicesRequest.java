package com.sbomfinder.dto;

public class CompareDevicesRequest {
    private Long device_1;
    private Long device_2;

    public Long getDevice_1() { return device_1; }
    public Long getDevice_2() { return device_2; }

    public void setDevice_1(Long device_1) { this.device_1 = device_1; }
    public void setDevice_2(Long device_2) { this.device_2 = device_2; }
}

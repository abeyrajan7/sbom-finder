package com.sbomfinder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sbomfinder.model.Device;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceNameAndManufacturer(String deviceName, String manufacturer);
    Optional<Device> findById(Long id);
}
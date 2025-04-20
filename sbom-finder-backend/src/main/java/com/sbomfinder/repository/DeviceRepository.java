package com.sbomfinder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sbomfinder.model.Device;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    @Query("SELECT d FROM Device d WHERE " +
            "(:query IS NULL OR LOWER(d.deviceName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(d.kernelVersion) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:manufacturer IS NULL OR :manufacturer = '' OR LOWER(d.manufacturer) = LOWER(:manufacturer)) AND " +
            "(:operatingSystem IS NULL OR :operatingSystem = '' OR LOWER(d.operatingSystem) = LOWER(:operatingSystem))")
    List<Device> searchWithFilters(@Param("query") String query,
                                   @Param("manufacturer") String manufacturer,
                                   @Param("operatingSystem") String operatingSystem);


    Optional<Device> findByDeviceNameAndManufacturer(String deviceName, String manufacturer);
    Optional<Device> findById(Long id);


}
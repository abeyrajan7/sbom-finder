package com.sbomfinder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sbomfinder.model.Device;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    //for fuzzy search
    @Query(value = "SELECT * FROM devices d WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "to_tsvector('english', d.device_name) @@ plainto_tsquery(:query) OR " +
            "similarity(d.device_name, :query) > 0.4 OR " +
            "similarity(d.manufacturer, :query) > 0.4 OR " +
            "similarity(d.operating_system, :query) > 0.4 OR " +
            "similarity(d.kernel_version, :query) > 0.4) AND " +
            "(:manufacturer IS NULL OR :manufacturer = '' OR d.manufacturer ILIKE :manufacturer) AND " +
            "(:operatingSystem IS NULL OR :operatingSystem = '' OR d.operating_system ILIKE :operatingSystem) AND " +
            "(:category IS NULL OR :category = '' OR d.category ILIKE :category) " +
            "ORDER BY similarity(d.device_name, :query) DESC, d.sbom_id DESC",
            nativeQuery = true)
    List<Device> searchWithFuzzyFilters(
            @Param("query") String query,
            @Param("manufacturer") String manufacturer,
            @Param("operatingSystem") String operatingSystem,
            @Param("category") String category);

    long countByCategory(String category);
    Optional<Device> findByDeviceNameAndManufacturer(String deviceName, String manufacturer);
    Optional<Device> findById(Long id);
    Optional<Device> findByDeviceNameAndManufacturerAndCategory(String deviceName, String manufacturer, String category);
}
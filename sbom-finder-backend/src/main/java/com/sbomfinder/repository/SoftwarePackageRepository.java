package com.sbomfinder.repository;

import com.sbomfinder.model.SoftwarePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.sbomfinder.model.Supplier;
import com.sbomfinder.repository.SupplierRepository;
import com.sbomfinder.model.Supplier;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SoftwarePackageRepository extends JpaRepository<SoftwarePackage, Long> {

    List<SoftwarePackage> findBySbomId(Long sbomId);
    boolean existsByNameAndVersion(String name, String version);

    @Query("SELECT sp FROM SoftwarePackage sp WHERE sp.sbom.id = :sbomId AND LOWER(sp.name) LIKE %:keyword%")
    List<SoftwarePackage> findPackagesBySbomIdAndKeyword(Long sbomId, String keyword);

    @Query("SELECT p FROM SoftwarePackage p LEFT JOIN FETCH p.supplier WHERE p.device.id = :deviceId")
    List<SoftwarePackage> findAllByDeviceIdWithSupplier(@Param("deviceId") Long deviceId);

    default List<SoftwarePackage> findFirmwareBySbomId(Long sbomId) {
        return findPackagesBySbomIdAndKeyword(sbomId, "firmware");
    }

    List<SoftwarePackage> findByDeviceId(Long deviceId);

    default List<SoftwarePackage> findOSBySbomId(Long sbomId) {
        return findPackagesBySbomIdAndKeyword(sbomId, "linux");
    }

    default List<SoftwarePackage> findDriversBySbomId(Long sbomId) {
        return findPackagesBySbomIdAndKeyword(sbomId, "driver");
    }

    List<SoftwarePackage> findBySupplier(Supplier supplier);

    void deleteByDeviceId(Long deviceId);

    long countBySupplierId(Long supplierId);

    @Query("SELECT sp FROM SoftwarePackage sp WHERE sp.sbom.device.deviceName = :deviceName AND sp.sbom.device.manufacturer = :manufacturer")
    List<SoftwarePackage> findByDeviceNameAndManufacturer(String deviceName, String manufacturer);
}



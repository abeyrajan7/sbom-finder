package com.sbomfinder.repository;

import com.sbomfinder.model.ExternalReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface ExternalReferenceRepository extends JpaRepository<ExternalReference, Long> {
    List<ExternalReference> findBySbomId(Long sbomId);
    boolean existsByReferenceLocator(String referenceLocator);
    @Query("SELECT er FROM ExternalReference er WHERE er.sbom.device.deviceName = :deviceName AND er.sbom.device.manufacturer = :manufacturer")
    List<ExternalReference> findByDeviceNameAndManufacturer(String deviceName, String manufacturer);

}

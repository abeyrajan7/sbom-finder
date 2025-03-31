package com.sbomfinder.repository;

import com.sbomfinder.model.Sbom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SbomRepository extends JpaRepository<Sbom, Long> {
    @Query("SELECT s FROM Sbom s WHERE s.device.id = :deviceId")
    Optional<Sbom> findByDeviceId(@Param("deviceId") Long deviceId);
    Optional<Sbom> findByDocumentNamespace(String documentNamespace);
}

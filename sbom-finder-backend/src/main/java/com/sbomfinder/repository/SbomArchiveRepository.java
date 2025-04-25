package com.sbomfinder.repository;

import com.sbomfinder.model.SbomArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SbomArchiveRepository extends JpaRepository<SbomArchive, Long> {
    Optional<SbomArchive> findTopByDeviceIdAndIsLatestTrue(Long deviceId);
    List<SbomArchive> findAllByDeviceId(Long deviceId);
    void deleteByDeviceId(Long deviceId);
}

package com.sbomfinder.repository;

import com.sbomfinder.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByName(String name);
    List<Supplier> findByPackagesIsEmpty();
    long countByPackagesIsEmpty();
}
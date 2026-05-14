package com.acme.vehicleops.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.Supplier;

@Repository
public interface SupplierJpaRepository extends JpaRepository<Supplier, Integer> {

    Optional<Supplier> findBySupplierCode(String supplierCode);
}

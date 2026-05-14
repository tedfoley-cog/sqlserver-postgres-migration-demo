package com.acme.vehicleops.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.SupplierShipment;

@Repository
public interface SupplierShipmentJpaRepository extends JpaRepository<SupplierShipment, Integer> {

    List<SupplierShipment> findBySupplierCodeAndShipDateGreaterThanEqual(
            String supplierCode, Date cutoffDate);

    List<SupplierShipment> findBySupplierCodeAndShipDateBetween(
            String supplierCode, Date from, Date to);
}

package com.acme.vehicleops.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.PartInventory;

@Repository
public interface PartInventoryJpaRepository extends JpaRepository<PartInventory, Integer> {

    List<PartInventory> findByPartNumber(String partNumber);

    @Query("SELECT SUM(pi.onHandQty) FROM PartInventory pi WHERE pi.partNumber = ?1")
    Integer sumOnHandQtyByPartNumber(String partNumber);

    @Query("SELECT COUNT(DISTINCT pi.warehouseCode) FROM PartInventory pi WHERE pi.partNumber = ?1")
    Integer countDistinctWarehouseByPartNumber(String partNumber);

    Optional<PartInventory> findFirstByPartNumberAndOnHandQtyGreaterThanOrderByOnHandQtyDesc(
            String partNumber, int minQty);
}

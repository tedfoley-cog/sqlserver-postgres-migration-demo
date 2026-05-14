package com.acme.vehicleops.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.ProductionTracking;

@Repository
public interface ProductionTrackingJpaRepository extends JpaRepository<ProductionTracking, Integer> {

    Optional<ProductionTracking> findByVehicleIdAndExitTimeIsNull(Integer vehicleId);

    long countByVehicleIdAndStatus(Integer vehicleId, String status);

    List<ProductionTracking> findByVehicleId(Integer vehicleId);

    List<ProductionTracking> findByStationIdAndStatusAndEntryTimeGreaterThanEqual(
            Integer stationId, String status, Date startDate);

    long countByStationIdAndExitTimeIsNull(Integer stationId);
}

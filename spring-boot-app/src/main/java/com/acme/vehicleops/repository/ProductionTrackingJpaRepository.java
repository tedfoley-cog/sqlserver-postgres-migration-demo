package com.acme.vehicleops.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (pt.exit_time - pt.entry_time)) / 60) " +
           "FROM production_tracking pt " +
           "WHERE pt.station_id = ?1 AND pt.status = 'COMPLETED' AND pt.exit_time IS NOT NULL",
           nativeQuery = true)
    Double avgCycleTimeMinutesByStation(Integer stationId);
}

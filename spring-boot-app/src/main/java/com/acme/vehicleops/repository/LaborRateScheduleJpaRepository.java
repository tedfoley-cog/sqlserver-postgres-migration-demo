package com.acme.vehicleops.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.LaborRateSchedule;

@Repository
public interface LaborRateScheduleJpaRepository extends JpaRepository<LaborRateSchedule, Integer> {

    @Query("SELECT l FROM LaborRateSchedule l " +
           "WHERE l.regionCode = ?1 AND l.dealerTier = ?2 AND l.laborType = ?3 " +
           "AND l.effectiveDate <= ?4 AND (l.expirationDate IS NULL OR l.expirationDate > ?4) " +
           "ORDER BY l.effectiveDate DESC")
    List<LaborRateSchedule> findCurrentRates(String regionCode, String dealerTier,
                                              String laborType, Date asOfDate);
}

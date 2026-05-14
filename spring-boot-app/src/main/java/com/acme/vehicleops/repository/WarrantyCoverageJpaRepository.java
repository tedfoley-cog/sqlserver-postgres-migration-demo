package com.acme.vehicleops.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.WarrantyCoverage;

@Repository
public interface WarrantyCoverageJpaRepository extends JpaRepository<WarrantyCoverage, Integer> {

    List<WarrantyCoverage> findByCoverageTypeAndModelYearAndMonthsFromSaleGreaterThanEqualAndMileageLimitGreaterThanEqualOrderByMonthsFromSaleDesc(
            String coverageType, Integer modelYear, Integer monthsFromSale, Integer mileageLimit);
}

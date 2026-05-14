package com.acme.vehicleops.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.ProductionStation;

@Repository
public interface ProductionStationJpaRepository extends JpaRepository<ProductionStation, Integer> {

    List<ProductionStation> findByPlantCodeOrderBySequenceOrderAsc(String plantCode);

    long countByPlantCode(String plantCode);

    Optional<ProductionStation> findFirstByPlantCodeAndIsQualityGateTrueOrderBySequenceOrderDesc(
            String plantCode);
}

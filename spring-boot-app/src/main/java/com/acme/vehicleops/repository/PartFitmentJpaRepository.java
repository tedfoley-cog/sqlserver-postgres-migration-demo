package com.acme.vehicleops.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.PartFitment;

@Repository
public interface PartFitmentJpaRepository extends JpaRepository<PartFitment, Integer> {

    List<PartFitment> findByPartNumberAndModelCode(String partNumber, String modelCode);
}

package com.acme.vehicleops.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.Vehicle;

@Repository
public interface VehicleJpaRepository extends JpaRepository<Vehicle, Integer> {

    Optional<Vehicle> findByVin(String vin);

    List<Vehicle> findByPlantCodeOrderByAssemblyDateDesc(String plantCode);

    List<Vehicle> findAllByOrderByVehicleIdAsc();

    long countByStatus(String status);
}

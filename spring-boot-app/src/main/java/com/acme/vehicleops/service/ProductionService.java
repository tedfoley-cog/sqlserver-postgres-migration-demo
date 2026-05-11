package com.acme.vehicleops.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.acme.vehicleops.model.Vehicle;
import com.acme.vehicleops.repository.ProductionRepository;

/**
 * Production service — thin delegation to stored procedure.
 *
 * Production status tracking, bottleneck detection, VIN assignment,
 * and station metrics are all computed inside sp_VehicleProductionStatus.
 * This service just passes the plant code and date range through.
 */
@Service
public class ProductionService {

    @Autowired
    private ProductionRepository productionRepository;

    public List<Vehicle> getVehicles(String plantCode) {
        if (plantCode != null && !plantCode.isEmpty()) {
            return productionRepository.getVehiclesByPlant(plantCode);
        }
        return productionRepository.getAllVehicles();
    }

    public Map<String, Object> getSummary() {
        return productionRepository.getProductionSummary();
    }
}

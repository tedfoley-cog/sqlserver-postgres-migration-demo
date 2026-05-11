package com.acme.vehicleops.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.vehicleops.model.Vehicle;
import com.acme.vehicleops.service.ProductionService;

@RestController
@RequestMapping("/api/production")
public class ProductionController {

    @Autowired
    private ProductionService productionService;

    @GetMapping("/vehicles")
    public List<Vehicle> getVehicles(
            @RequestParam(required = false) String plantCode) {
        return productionService.getVehicles(plantCode);
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return productionService.getSummary();
    }
}

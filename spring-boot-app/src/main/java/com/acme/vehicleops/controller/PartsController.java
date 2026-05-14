package com.acme.vehicleops.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.vehicleops.model.Part;
import com.acme.vehicleops.service.PartsService;

@RestController
@RequestMapping("/api/parts")
public class PartsController {

    @Autowired
    private PartsService partsService;

    @GetMapping
    public List<Part> getAllParts() {
        return partsService.getAllParts();
    }

    @GetMapping("/supersession")
    public List<Map<String, Object>> getSupersessionChain(
            @RequestParam String partNumber,
            @RequestParam(required = false) String vin) {
        if (vin != null && !vin.isEmpty()) {
            return partsService.getSupersessionChainForVehicle(partNumber, vin);
        }
        return partsService.getSupersessionChain(partNumber);
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return partsService.getPartsSummary();
    }
}

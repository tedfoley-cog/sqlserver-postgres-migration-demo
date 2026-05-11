package com.acme.vehicleops.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.vehicleops.model.WarrantyClaim;
import com.acme.vehicleops.service.WarrantyService;

@RestController
@RequestMapping("/api/warranty")
public class WarrantyController {

    @Autowired
    private WarrantyService warrantyService;

    @GetMapping("/claims")
    public List<WarrantyClaim> getRecentClaims() {
        return warrantyService.getRecentClaims();
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return warrantyService.getSummary();
    }

    @PostMapping("/claims")
    public Map<String, Object> submitClaim(
            @RequestParam String vin,
            @RequestParam String dealerCode,
            @RequestParam int mileage,
            @RequestParam String symptomCode,
            @RequestParam String partNumber,
            @RequestParam String laborOp,
            @RequestParam BigDecimal laborHours,
            @RequestParam(defaultValue = "0") BigDecimal subletCost,
            @RequestParam String submittedBy) {
        return warrantyService.submitClaim(
            vin, dealerCode, mileage, symptomCode,
            partNumber, laborOp, laborHours, subletCost, submittedBy);
    }
}

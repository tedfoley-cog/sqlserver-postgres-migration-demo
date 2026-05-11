package com.acme.vehicleops.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.acme.vehicleops.model.WarrantyClaim;
import com.acme.vehicleops.repository.WarrantyRepository;

/**
 * Warranty service — thin delegation to stored procedure.
 *
 * All warranty business logic (coverage determination, deductible calculation,
 * labor rate lookup, claim validation) is in sp_ProcessWarrantyClaim.
 * This service just passes parameters through.
 */
@Service
public class WarrantyService {

    @Autowired
    private WarrantyRepository warrantyRepository;

    public Map<String, Object> submitClaim(
            String vin,
            String dealerCode,
            int mileage,
            String symptomCode,
            String partNumber,
            String laborOp,
            BigDecimal laborHours,
            BigDecimal subletCost,
            String submittedBy) {
        // All validation and business logic is in the stored procedure
        return warrantyRepository.processWarrantyClaim(
            vin, dealerCode, mileage, symptomCode,
            partNumber, laborOp, laborHours, subletCost, submittedBy);
    }

    public List<WarrantyClaim> getRecentClaims() {
        return warrantyRepository.getRecentClaims(20);
    }

    public Map<String, Object> getSummary() {
        return warrantyRepository.getClaimSummary();
    }
}

package com.acme.vehicleops.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acme.vehicleops.model.LaborRateSchedule;
import com.acme.vehicleops.model.Part;
import com.acme.vehicleops.model.Vehicle;
import com.acme.vehicleops.model.WarrantyClaim;
import com.acme.vehicleops.model.WarrantyCoverage;
import com.acme.vehicleops.repository.LaborRateScheduleJpaRepository;
import com.acme.vehicleops.repository.PartJpaRepository;
import com.acme.vehicleops.repository.VehicleJpaRepository;
import com.acme.vehicleops.repository.WarrantyClaimJpaRepository;
import com.acme.vehicleops.repository.WarrantyCoverageJpaRepository;

/**
 * Warranty claim processing — extracted from sp_ProcessWarrantyClaim.
 *
 * Business rules extracted:
 *   - Coverage priority: EMISSIONS > POWERTRAIN > CORROSION > BUMPER_TO_BUMPER
 *   - Dealer region mapping from dealer code prefix
 *   - Dealer tier mapping from dealer code suffix
 *   - Labor rate lookup by region + tier + part group
 *   - High-value claim threshold ($2500) triggers manual review
 *   - Deductible application per coverage type
 *   - Fallback labor rate: $85/hr
 */
@Service
public class WarrantyService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("2500.00");
    private static final BigDecimal DEFAULT_LABOR_RATE = new BigDecimal("85.00");
    private static final String[] COVERAGE_PRIORITY = {"EMISSIONS", "POWERTRAIN", "CORROSION", "BUMPER_TO_BUMPER"};

    private final WarrantyClaimJpaRepository claimRepository;
    private final VehicleJpaRepository vehicleRepository;
    private final PartJpaRepository partRepository;
    private final WarrantyCoverageJpaRepository coverageRepository;
    private final LaborRateScheduleJpaRepository laborRateRepository;

    public WarrantyService(WarrantyClaimJpaRepository claimRepository,
                           VehicleJpaRepository vehicleRepository,
                           PartJpaRepository partRepository,
                           WarrantyCoverageJpaRepository coverageRepository,
                           LaborRateScheduleJpaRepository laborRateRepository) {
        this.claimRepository = claimRepository;
        this.vehicleRepository = vehicleRepository;
        this.partRepository = partRepository;
        this.coverageRepository = coverageRepository;
        this.laborRateRepository = laborRateRepository;
    }

    @Transactional
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

        Map<String, Object> result = new HashMap<>();
        result.put("claimStatus", "DENIED");
        result.put("denialReason", "");
        result.put("totalAmount", BigDecimal.ZERO);

        // Step 1: Validate vehicle
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByVin(vin);
        if (!vehicleOpt.isPresent()) {
            result.put("denialReason", "DENY_01: Vehicle not found for VIN " + vin);
            return result;
        }
        Vehicle vehicle = vehicleOpt.get();
        if (vehicle.getWarrantyStartDate() == null) {
            result.put("denialReason", "DENY_02: No warranty start date on file for VIN " + vin);
            return result;
        }

        // Step 2: Calculate months since warranty start
        int monthsSinceSale = calculateMonthsBetween(vehicle.getWarrantyStartDate(), new Date());

        // Step 3: Determine coverage type
        Optional<Part> partOpt = partRepository.findByPartNumber(partNumber);
        if (!partOpt.isPresent()) {
            result.put("denialReason", "DENY_03: Causal part number not found: " + partNumber);
            return result;
        }
        String partGroupCode = partOpt.get().getPartGroupCode();

        WarrantyCoverage coverage = findApplicableCoverage(
                vehicle.getModelYear(), monthsSinceSale, mileage, partGroupCode);

        if (coverage == null) {
            result.put("denialReason", "DENY_04: No applicable coverage for part group "
                    + partGroupCode + " at " + monthsSinceSale + " months / " + mileage + " miles");
            return result;
        }

        // Step 4: Labor rate lookup
        String dealerRegion = determineDealerRegion(dealerCode);
        String dealerTier = determineDealerTier(dealerCode);
        String laborType = determineLaborType(partGroupCode);

        List<LaborRateSchedule> rates = laborRateRepository
                .findCurrentRates(dealerRegion, dealerTier, laborType, new Date());
        BigDecimal laborRate = rates.isEmpty() ? DEFAULT_LABOR_RATE : rates.get(0).getHourlyRate();

        // Step 5: Calculate costs
        BigDecimal laborCost = laborHours.multiply(laborRate);
        BigDecimal partsCost = partOpt.get().getUnitCost() != null
                ? partOpt.get().getUnitCost() : BigDecimal.ZERO;
        BigDecimal deductible = coverage.getDeductibleAmount() != null
                ? coverage.getDeductibleAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = laborCost.add(partsCost)
                .add(subletCost != null ? subletCost : BigDecimal.ZERO)
                .subtract(deductible);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // Step 6: High-value check
        boolean isHighValue = totalAmount.compareTo(HIGH_VALUE_THRESHOLD) > 0;

        // Step 7: Generate claim and insert
        String claimNumber = generateClaimNumber();
        String claimStatus = isHighValue ? "SUBMITTED" : "APPROVED";

        WarrantyClaim claim = new WarrantyClaim();
        claim.setClaimNumber(claimNumber);
        claim.setVehicleId(vehicle.getVehicleId());
        claim.setDealerCode(dealerCode);
        claim.setClaimDate(new Date());
        claim.setMileageAtClaim(mileage);
        claim.setSymptomCode(symptomCode);
        claim.setCausalPartNumber(partNumber);
        claim.setLaborOperationCode(laborOp);
        claim.setLaborHours(laborHours);
        claim.setLaborRate(laborRate);
        claim.setPartsCost(partsCost);
        claim.setSubletCost(subletCost != null ? subletCost : BigDecimal.ZERO);
        claim.setDeductible(deductible);
        claim.setTotalAmount(totalAmount);
        claim.setCoverageType(coverage.getCoverageType());
        claim.setClaimStatus(claimStatus);
        claim.setApprovedDate("APPROVED".equals(claimStatus) ? new Date() : null);
        claim.setSubmittedBy(submittedBy);
        claimRepository.save(claim);

        // Step 8: Update vehicle mileage
        if (vehicle.getCurrentMileage() == null || vehicle.getCurrentMileage() < mileage) {
            vehicle.setCurrentMileage(mileage);
            vehicleRepository.save(vehicle);
        }

        result.put("claimNumber", claimNumber);
        result.put("claimStatus", claimStatus);
        result.put("totalAmount", totalAmount);
        result.put("coverageType", coverage.getCoverageType());
        result.put("laborRate", laborRate);
        result.put("denialReason", "");
        return result;
    }

    public List<WarrantyClaim> getRecentClaims() {
        return claimRepository.findAllByOrderByClaimDateDesc(PageRequest.of(0, 20));
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalClaims", claimRepository.count());
        summary.put("approvedClaims", claimRepository.countByClaimStatus("APPROVED"));
        summary.put("deniedClaims", claimRepository.countByClaimStatus("DENIED"));
        summary.put("totalAmount", claimRepository.sumTotalAmountByClaimStatus("APPROVED"));
        return summary;
    }

    WarrantyCoverage findApplicableCoverage(int modelYear, int monthsSinceSale,
                                            int mileageAtClaim, String partGroupCode) {
        for (String coverageType : COVERAGE_PRIORITY) {
            List<WarrantyCoverage> coverages = coverageRepository
                    .findByCoverageTypeAndModelYearAndMonthsFromSaleGreaterThanEqualAndMileageLimitGreaterThanEqualOrderByMonthsFromSaleDesc(
                            coverageType, modelYear, monthsSinceSale, mileageAtClaim);
            for (WarrantyCoverage cov : coverages) {
                if (partGroupCovers(cov, partGroupCode, coverageType)) {
                    return cov;
                }
            }
        }
        return null;
    }

    private boolean partGroupCovers(WarrantyCoverage coverage, String partGroupCode,
                                    String coverageType) {
        String codes = coverage.getPartGroupCodes();
        if ("BUMPER_TO_BUMPER".equals(coverageType) && (codes == null || codes.isEmpty())) {
            return true;
        }
        if (codes == null || codes.isEmpty()) {
            return false;
        }
        return Arrays.asList(codes.split(",")).contains(partGroupCode);
    }

    static String determineDealerRegion(String dealerCode) {
        if (dealerCode == null || dealerCode.length() < 2) return "MIDWEST";
        String prefix = dealerCode.substring(0, 2);
        switch (prefix) {
            case "NE": case "NY": case "CT": case "MA": return "NORTHEAST";
            case "SE": case "FL": case "GA": case "NC": return "SOUTHEAST";
            case "MW": case "IL": case "OH": case "MI": return "MIDWEST";
            case "SW": case "TX": case "AZ": case "NM": return "SOUTHWEST";
            case "WE": case "CA": case "WA": case "OR": return "WEST";
            default: return "MIDWEST";
        }
    }

    static String determineDealerTier(String dealerCode) {
        if (dealerCode == null || dealerCode.isEmpty()) return "C";
        char lastChar = dealerCode.charAt(dealerCode.length() - 1);
        if (lastChar >= '1' && lastChar <= '3') return "A";
        if (lastChar >= '4' && lastChar <= '6') return "B";
        return "C";
    }

    static String determineLaborType(String partGroupCode) {
        if (partGroupCode == null) return "MECHANICAL";
        switch (partGroupCode) {
            case "ECU": case "WIR": case "BAT": return "ELECTRICAL";
            case "ENG": case "TRN": case "DRV": return "MECHANICAL";
            default: return "MECHANICAL";
        }
    }

    private String generateClaimNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        int nextId = claimRepository.findMaxClaimId() + 1;
        return "WC-" + sdf.format(new Date()) + "-" + String.format("%04d", nextId);
    }

    static int calculateMonthsBetween(Date start, Date end) {
        java.util.Calendar startCal = java.util.Calendar.getInstance();
        startCal.setTime(start);
        java.util.Calendar endCal = java.util.Calendar.getInstance();
        endCal.setTime(end);
        int months = (endCal.get(java.util.Calendar.YEAR) - startCal.get(java.util.Calendar.YEAR)) * 12;
        months += endCal.get(java.util.Calendar.MONTH) - startCal.get(java.util.Calendar.MONTH);
        return months;
    }
}

package com.acme.vehicleops.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acme.vehicleops.model.DealerSettlement;
import com.acme.vehicleops.model.Part;
import com.acme.vehicleops.model.WarrantyClaim;
import com.acme.vehicleops.repository.DealerSettlementJpaRepository;
import com.acme.vehicleops.repository.PartJpaRepository;
import com.acme.vehicleops.repository.WarrantyClaimJpaRepository;

/**
 * Dealer settlement batch processing — extracted from sp_DealerSettlement.
 *
 * Business rules extracted:
 *   - Batch number format: STL-yyyyMMdd-###
 *   - Parts markup by supplier type: OEM 40%, REMAN 25%, AFT 15%, default 30%
 *   - Markup capped at list price
 *   - Sublet cost: pass-through if <= $500, else 80% cap
 *   - Updates claim status to SETTLED
 */
@Service
public class DealerSettlementService {

    private static final BigDecimal SUBLET_PASSTHROUGH_LIMIT = new BigDecimal("500.00");
    private static final BigDecimal SUBLET_CAP_PERCENT = new BigDecimal("0.80");

    private final WarrantyClaimJpaRepository claimRepository;
    private final PartJpaRepository partRepository;
    private final DealerSettlementJpaRepository settlementRepository;

    public DealerSettlementService(WarrantyClaimJpaRepository claimRepository,
                                   PartJpaRepository partRepository,
                                   DealerSettlementJpaRepository settlementRepository) {
        this.claimRepository = claimRepository;
        this.partRepository = partRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public Map<String, Object> runSettlementBatch() {
        String batchNumber = generateBatchNumber();
        List<WarrantyClaim> claims = claimRepository
                .findByClaimStatusAndSettledDateIsNullForUpdate("APPROVED");

        List<DealerSettlement> settlements = new ArrayList<>();
        BigDecimal batchTotal = BigDecimal.ZERO;

        for (WarrantyClaim claim : claims) {
            DealerSettlement settlement = processClaimSettlement(claim, batchNumber);
            settlements.add(settlement);
            batchTotal = batchTotal.add(settlement.getNetAmount());

            claim.setClaimStatus("SETTLED");
            claim.setSettledDate(new Date());
            claimRepository.save(claim);
        }

        settlementRepository.saveAll(settlements);

        Map<String, Object> result = new HashMap<>();
        result.put("batchNumber", batchNumber);
        result.put("claimsProcessed", settlements.size());
        result.put("batchTotal", batchTotal);
        return result;
    }

    DealerSettlement processClaimSettlement(WarrantyClaim claim, String batchNumber) {
        BigDecimal laborAmount = BigDecimal.ZERO;
        if (claim.getLaborHours() != null && claim.getLaborRate() != null) {
            laborAmount = claim.getLaborHours().multiply(claim.getLaborRate());
        }

        BigDecimal partsAmount = claim.getPartsCost() != null
                ? claim.getPartsCost() : BigDecimal.ZERO;

        BigDecimal partsMarkup = BigDecimal.ZERO;
        if (claim.getCausalPartNumber() != null) {
            Part part = partRepository.findByPartNumber(claim.getCausalPartNumber()).orElse(null);
            if (part != null) {
                BigDecimal markupPct = determinePartsMarkupPct(part.getSupplierCode());
                partsMarkup = partsAmount.multiply(markupPct)
                        .setScale(2, RoundingMode.HALF_UP);
                if (part.getListPrice() != null) {
                    BigDecimal maxMarkup = part.getListPrice().subtract(partsAmount);
                    if (maxMarkup.compareTo(BigDecimal.ZERO) <= 0) {
                        partsMarkup = BigDecimal.ZERO;
                    } else if (partsMarkup.compareTo(maxMarkup) > 0) {
                        partsMarkup = maxMarkup;
                    }
                }
            }
        }

        BigDecimal subletAmount = calculateSubletAllowance(
                claim.getSubletCost() != null ? claim.getSubletCost() : BigDecimal.ZERO);

        BigDecimal deductibleCredit = claim.getDeductible() != null
                ? claim.getDeductible() : BigDecimal.ZERO;

        BigDecimal netAmount = laborAmount
                .add(partsAmount)
                .add(partsMarkup)
                .add(subletAmount)
                .subtract(deductibleCredit);
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            netAmount = BigDecimal.ZERO;
        }

        DealerSettlement settlement = new DealerSettlement();
        settlement.setSettlementBatchNo(batchNumber);
        settlement.setDealerCode(claim.getDealerCode());
        settlement.setClaimId(claim.getClaimId());
        settlement.setLaborAmount(laborAmount);
        settlement.setPartsAmount(partsAmount);
        settlement.setPartsMarkup(partsMarkup);
        settlement.setSubletAmount(subletAmount);
        settlement.setDeductibleCredit(deductibleCredit);
        settlement.setNetAmount(netAmount);
        settlement.setSettlementDate(new Date());
        settlement.setPaymentStatus("PENDING");
        return settlement;
    }

    static BigDecimal determinePartsMarkupPct(String supplierCode) {
        if (supplierCode == null) return new BigDecimal("0.30");
        if (supplierCode.startsWith("OEM")) return new BigDecimal("0.40");
        if (supplierCode.startsWith("REMAN")) return new BigDecimal("0.25");
        if (supplierCode.startsWith("AFT")) return new BigDecimal("0.15");
        return new BigDecimal("0.30");
    }

    static BigDecimal calculateSubletAllowance(BigDecimal subletCost) {
        if (subletCost == null || subletCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (subletCost.compareTo(SUBLET_PASSTHROUGH_LIMIT) <= 0) {
            return subletCost;
        }
        return subletCost.multiply(SUBLET_CAP_PERCENT).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateBatchNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String prefix = "STL-" + sdf.format(new Date());
        long count = settlementRepository.countBySettlementBatchNoStartingWith(prefix);
        return prefix + "-" + String.format("%03d", count + 1);
    }
}

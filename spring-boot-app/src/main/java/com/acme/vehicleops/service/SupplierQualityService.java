package com.acme.vehicleops.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.acme.vehicleops.model.Supplier;
import com.acme.vehicleops.model.SupplierShipment;
import com.acme.vehicleops.repository.SupplierJpaRepository;
import com.acme.vehicleops.repository.SupplierShipmentJpaRepository;

/**
 * Supplier quality scorecard — extracted from sp_SupplierQualityScorecard.
 *
 * Weights: PPM 30%, OTD 30%, Rejection 25%, Certification 15%.
 * Rating: GREEN >= 90, YELLOW >= 70, RED < 70.
 * Trend: improving if PPM down 10%+ AND OTD up 2%+,
 *         declining if PPM up 10%+ OR OTD down 5%+.
 */
@Service
public class SupplierQualityService {

    static final BigDecimal WEIGHT_PPM = new BigDecimal("0.30");
    static final BigDecimal WEIGHT_OTD = new BigDecimal("0.30");
    static final BigDecimal WEIGHT_REJECTION = new BigDecimal("0.25");
    static final BigDecimal WEIGHT_CERT = new BigDecimal("0.15");

    private final SupplierJpaRepository supplierRepository;
    private final SupplierShipmentJpaRepository shipmentRepository;

    public SupplierQualityService(SupplierJpaRepository supplierRepository,
                                  SupplierShipmentJpaRepository shipmentRepository) {
        this.supplierRepository = supplierRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public List<Map<String, Object>> generateScorecards() {
        List<Supplier> suppliers = supplierRepository.findAll();
        List<Map<String, Object>> scorecards = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MONTH, -12);
        Date twelveMonthsAgo = cal.getTime();

        for (Supplier supplier : suppliers) {
            Map<String, Object> card = buildScorecard(supplier, twelveMonthsAgo, now);
            scorecards.add(card);
        }
        return scorecards;
    }

    Map<String, Object> buildScorecard(Supplier supplier, Date from, Date to) {
        Map<String, Object> card = new HashMap<>();
        card.put("supplierCode", supplier.getSupplierCode());
        card.put("supplierName", supplier.getSupplierName());
        card.put("region", supplier.getRegion());

        List<SupplierShipment> shipments = shipmentRepository
                .findBySupplierCodeAndShipDateBetween(supplier.getSupplierCode(), from, to);

        int totalOrdered = 0;
        int totalReceived = 0;
        int totalRejected = 0;
        int onTimeCount = 0;

        for (SupplierShipment s : shipments) {
            totalOrdered += s.getOrderedQty() != null ? s.getOrderedQty() : 0;
            totalReceived += s.getReceivedQty() != null ? s.getReceivedQty() : 0;
            totalRejected += s.getRejectedQty() != null ? s.getRejectedQty() : 0;
            if (Boolean.TRUE.equals(s.getIsOnTime())) {
                onTimeCount++;
            }
        }

        int ppm = totalReceived > 0
                ? (int) ((long) totalRejected * 1_000_000 / totalReceived) : 0;
        BigDecimal otdPct = shipments.isEmpty() ? BigDecimal.ZERO
                : new BigDecimal(onTimeCount).multiply(new BigDecimal(100))
                        .divide(new BigDecimal(shipments.size()), 2, RoundingMode.HALF_UP);
        BigDecimal rejectionRate = totalReceived > 0
                ? new BigDecimal(totalRejected).multiply(new BigDecimal(100))
                        .divide(new BigDecimal(totalReceived), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal ppmScore = scorePpm(ppm);
        BigDecimal otdScore = scoreOtd(otdPct);
        BigDecimal rejectionScore = scoreRejection(rejectionRate);
        BigDecimal certScore = scoreCertification(supplier);

        BigDecimal compositeScore = ppmScore.multiply(WEIGHT_PPM)
                .add(otdScore.multiply(WEIGHT_OTD))
                .add(rejectionScore.multiply(WEIGHT_REJECTION))
                .add(certScore.multiply(WEIGHT_CERT))
                .setScale(2, RoundingMode.HALF_UP);

        String rating = determineRating(compositeScore);

        card.put("ppm", ppm);
        card.put("onTimeDeliveryPct", otdPct);
        card.put("rejectionRate", rejectionRate);
        card.put("ppmScore", ppmScore);
        card.put("otdScore", otdScore);
        card.put("rejectionScore", rejectionScore);
        card.put("certScore", certScore);
        card.put("compositeScore", compositeScore);
        card.put("rating", rating);
        card.put("shipmentCount", shipments.size());

        return card;
    }

    static BigDecimal scorePpm(int ppm) {
        if (ppm == 0) return new BigDecimal("100");
        if (ppm <= 100) return new BigDecimal("95");
        if (ppm <= 500) return new BigDecimal("80");
        if (ppm <= 1000) return new BigDecimal("60");
        if (ppm <= 2500) return new BigDecimal("40");
        if (ppm <= 5000) return new BigDecimal("20");
        return BigDecimal.ZERO;
    }

    static BigDecimal scoreOtd(BigDecimal otdPct) {
        if (otdPct == null) return BigDecimal.ZERO;
        return otdPct.min(new BigDecimal("100"));
    }

    static BigDecimal scoreRejection(BigDecimal rejectionRate) {
        if (rejectionRate == null || rejectionRate.compareTo(BigDecimal.ZERO) == 0)
            return new BigDecimal("100");
        BigDecimal score = new BigDecimal("100").subtract(rejectionRate.multiply(new BigDecimal("10")));
        return score.max(BigDecimal.ZERO);
    }

    static BigDecimal scoreCertification(Supplier supplier) {
        if (Boolean.FALSE.equals(supplier.getIsApproved())) return BigDecimal.ZERO;
        if (supplier.getCertificationExpiry() == null) return new BigDecimal("50");
        Date now = new Date();
        if (supplier.getCertificationExpiry().before(now)) return new BigDecimal("25");
        long daysUntilExpiry = (supplier.getCertificationExpiry().getTime() - now.getTime())
                / (1000L * 60 * 60 * 24);
        if (daysUntilExpiry > 180) return new BigDecimal("100");
        if (daysUntilExpiry > 90) return new BigDecimal("80");
        return new BigDecimal("60");
    }

    static String determineRating(BigDecimal compositeScore) {
        if (compositeScore.compareTo(new BigDecimal("90")) >= 0) return "GREEN";
        if (compositeScore.compareTo(new BigDecimal("70")) >= 0) return "YELLOW";
        return "RED";
    }
}

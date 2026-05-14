package com.acme.vehicleops.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.acme.vehicleops.model.Supplier;

class SupplierQualityServiceTest {

    @Test
    void scorePpmZero() {
        assertEquals(new BigDecimal("100"), SupplierQualityService.scorePpm(0));
    }

    @Test
    void scorePpmLow() {
        assertEquals(new BigDecimal("95"), SupplierQualityService.scorePpm(50));
        assertEquals(new BigDecimal("95"), SupplierQualityService.scorePpm(100));
    }

    @Test
    void scorePpmMedium() {
        assertEquals(new BigDecimal("80"), SupplierQualityService.scorePpm(250));
        assertEquals(new BigDecimal("80"), SupplierQualityService.scorePpm(500));
    }

    @Test
    void scorePpmHigh() {
        assertEquals(new BigDecimal("60"), SupplierQualityService.scorePpm(750));
        assertEquals(new BigDecimal("60"), SupplierQualityService.scorePpm(1000));
    }

    @Test
    void scorePpmVeryHigh() {
        assertEquals(new BigDecimal("40"), SupplierQualityService.scorePpm(1500));
        assertEquals(new BigDecimal("40"), SupplierQualityService.scorePpm(2500));
    }

    @Test
    void scorePpmExtreme() {
        assertEquals(new BigDecimal("20"), SupplierQualityService.scorePpm(3000));
        assertEquals(new BigDecimal("20"), SupplierQualityService.scorePpm(5000));
    }

    @Test
    void scorePpmAboveFiveThousand() {
        assertEquals(BigDecimal.ZERO, SupplierQualityService.scorePpm(6000));
    }

    @Test
    void scoreOtdNormal() {
        assertEquals(new BigDecimal("95.00"), SupplierQualityService.scoreOtd(new BigDecimal("95.00")));
    }

    @Test
    void scoreOtdCapsAt100() {
        assertEquals(new BigDecimal("100"), SupplierQualityService.scoreOtd(new BigDecimal("105")));
    }

    @Test
    void scoreOtdNull() {
        assertEquals(BigDecimal.ZERO, SupplierQualityService.scoreOtd(null));
    }

    @Test
    void scoreRejectionZero() {
        assertEquals(new BigDecimal("100"), SupplierQualityService.scoreRejection(BigDecimal.ZERO));
    }

    @Test
    void scoreRejectionFivePercent() {
        assertEquals(new BigDecimal("50"), SupplierQualityService.scoreRejection(new BigDecimal("5")));
    }

    @Test
    void scoreRejectionTenPercent() {
        assertEquals(BigDecimal.ZERO, SupplierQualityService.scoreRejection(new BigDecimal("10")));
    }

    @Test
    void scoreRejectionOverTenFlooredAtZero() {
        assertEquals(BigDecimal.ZERO, SupplierQualityService.scoreRejection(new BigDecimal("15")));
    }

    @Test
    void ratingGreen() {
        assertEquals("GREEN", SupplierQualityService.determineRating(new BigDecimal("90")));
        assertEquals("GREEN", SupplierQualityService.determineRating(new BigDecimal("95")));
    }

    @Test
    void ratingYellow() {
        assertEquals("YELLOW", SupplierQualityService.determineRating(new BigDecimal("70")));
        assertEquals("YELLOW", SupplierQualityService.determineRating(new BigDecimal("85")));
    }

    @Test
    void ratingRed() {
        assertEquals("RED", SupplierQualityService.determineRating(new BigDecimal("69")));
        assertEquals("RED", SupplierQualityService.determineRating(new BigDecimal("50")));
    }

    @Test
    void certScoreNotApproved() {
        Supplier s = new Supplier();
        s.setIsApproved(false);
        assertEquals(BigDecimal.ZERO, SupplierQualityService.scoreCertification(s));
    }

    @Test
    void certScoreNoExpiry() {
        Supplier s = new Supplier();
        s.setIsApproved(true);
        s.setCertificationExpiry(null);
        assertEquals(new BigDecimal("50"), SupplierQualityService.scoreCertification(s));
    }

    @Test
    void certScoreExpired() {
        Supplier s = new Supplier();
        s.setIsApproved(true);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        s.setCertificationExpiry(cal.getTime());
        assertEquals(new BigDecimal("25"), SupplierQualityService.scoreCertification(s));
    }

    @Test
    void certScoreFarFuture() {
        Supplier s = new Supplier();
        s.setIsApproved(true);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 365);
        s.setCertificationExpiry(cal.getTime());
        assertEquals(new BigDecimal("100"), SupplierQualityService.scoreCertification(s));
    }

    @Test
    void certScoreMidRange() {
        Supplier s = new Supplier();
        s.setIsApproved(true);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 120);
        s.setCertificationExpiry(cal.getTime());
        assertEquals(new BigDecimal("80"), SupplierQualityService.scoreCertification(s));
    }

    @Test
    void certScoreSoonExpiring() {
        Supplier s = new Supplier();
        s.setIsApproved(true);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 45);
        s.setCertificationExpiry(cal.getTime());
        assertEquals(new BigDecimal("60"), SupplierQualityService.scoreCertification(s));
    }
}

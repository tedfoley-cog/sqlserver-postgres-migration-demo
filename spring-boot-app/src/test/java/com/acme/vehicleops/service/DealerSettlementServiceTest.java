package com.acme.vehicleops.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DealerSettlementServiceTest {

    @Test
    void markupPctOem() {
        assertEquals(new BigDecimal("0.40"),
                DealerSettlementService.determinePartsMarkupPct("OEM-CAST-01"));
    }

    @Test
    void markupPctReman() {
        assertEquals(new BigDecimal("0.25"),
                DealerSettlementService.determinePartsMarkupPct("REMAN-RAD-01"));
    }

    @Test
    void markupPctAftermarket() {
        assertEquals(new BigDecimal("0.15"),
                DealerSettlementService.determinePartsMarkupPct("AFT-BATT-01"));
    }

    @Test
    void markupPctDefault() {
        assertEquals(new BigDecimal("0.30"),
                DealerSettlementService.determinePartsMarkupPct("UNKNOWN-01"));
    }

    @Test
    void markupPctNull() {
        assertEquals(new BigDecimal("0.30"),
                DealerSettlementService.determinePartsMarkupPct(null));
    }

    @Test
    void subletPassthroughUnder500() {
        BigDecimal result = DealerSettlementService.calculateSubletAllowance(
                new BigDecimal("200.00"));
        assertEquals(new BigDecimal("200.00"), result);
    }

    @Test
    void subletPassthroughAt500() {
        BigDecimal result = DealerSettlementService.calculateSubletAllowance(
                new BigDecimal("500.00"));
        assertEquals(new BigDecimal("500.00"), result);
    }

    @Test
    void subletCappedOver500() {
        BigDecimal result = DealerSettlementService.calculateSubletAllowance(
                new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("800.00"), result);
    }

    @Test
    void subletZero() {
        assertEquals(BigDecimal.ZERO,
                DealerSettlementService.calculateSubletAllowance(BigDecimal.ZERO));
    }

    @Test
    void subletNull() {
        assertEquals(BigDecimal.ZERO,
                DealerSettlementService.calculateSubletAllowance(null));
    }

    @Test
    void subletNegative() {
        assertEquals(BigDecimal.ZERO,
                DealerSettlementService.calculateSubletAllowance(new BigDecimal("-50")));
    }
}

package com.acme.vehicleops.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductionServiceTest {

    @Test
    void bottleneckGreenNoVariance() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(new BigDecimal("45"), new BigDecimal("45")));
    }

    @Test
    void bottleneckGreenLowVariance() {
        // 5% variance: (47.25 - 45) / 45 = 0.05
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(new BigDecimal("47.25"), new BigDecimal("45")));
    }

    @Test
    void bottleneckYellowModerateVariance() {
        // 15% variance: (51.75 - 45) / 45 = 0.15
        assertEquals("YELLOW",
                ProductionService.determineBottleneckFlag(new BigDecimal("51.75"), new BigDecimal("45")));
    }

    @Test
    void bottleneckRedHighVariance() {
        // 30% variance: (58.5 - 45) / 45 = 0.30
        assertEquals("RED",
                ProductionService.determineBottleneckFlag(new BigDecimal("58.5"), new BigDecimal("45")));
    }

    @Test
    void bottleneckGreenNullCycleTime() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(new BigDecimal("50"), null));
    }

    @Test
    void bottleneckGreenZeroCycleTime() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(new BigDecimal("50"), BigDecimal.ZERO));
    }

    @Test
    void bottleneckGreenZeroActual() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(BigDecimal.ZERO, new BigDecimal("45")));
    }

    @Test
    void bottleneckGreenNullActual() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(null, new BigDecimal("45")));
    }

    @Test
    void bottleneckYellowBoundary() {
        // Exactly 10% variance: (49.5 - 45) / 45 = 0.10 — not > 0.10, so GREEN
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(new BigDecimal("49.5"), new BigDecimal("45")));
    }

    @Test
    void bottleneckRedBoundary() {
        // Exactly 25% variance: (56.25 - 45) / 45 = 0.25 — not > 0.25, so YELLOW
        assertEquals("YELLOW",
                ProductionService.determineBottleneckFlag(new BigDecimal("56.25"), new BigDecimal("45")));
    }
}

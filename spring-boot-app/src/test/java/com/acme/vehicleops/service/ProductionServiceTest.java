package com.acme.vehicleops.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductionServiceTest {

    @Test
    void bottleneckGreenNoLoad() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(0, new BigDecimal("45")));
    }

    @Test
    void bottleneckGreenLowLoad() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(1, new BigDecimal("45")));
    }

    @Test
    void bottleneckYellowMediumLoad() {
        assertEquals("YELLOW",
                ProductionService.determineBottleneckFlag(2, new BigDecimal("45")));
    }

    @Test
    void bottleneckRedHighLoad() {
        assertEquals("RED",
                ProductionService.determineBottleneckFlag(4, new BigDecimal("45")));
    }

    @Test
    void bottleneckGreenNullCycleTime() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(5, null));
    }

    @Test
    void bottleneckGreenZeroCycleTime() {
        assertEquals("GREEN",
                ProductionService.determineBottleneckFlag(5, BigDecimal.ZERO));
    }
}

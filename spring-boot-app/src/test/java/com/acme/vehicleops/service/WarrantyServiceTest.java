package com.acme.vehicleops.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

class WarrantyServiceTest {

    @Test
    void dealerRegionNortheast() {
        assertEquals("NORTHEAST", WarrantyService.determineDealerRegion("NE-NY-1"));
        assertEquals("NORTHEAST", WarrantyService.determineDealerRegion("NY-001"));
        assertEquals("NORTHEAST", WarrantyService.determineDealerRegion("CT-005"));
        assertEquals("NORTHEAST", WarrantyService.determineDealerRegion("MA-123"));
    }

    @Test
    void dealerRegionSoutheast() {
        assertEquals("SOUTHEAST", WarrantyService.determineDealerRegion("SE-FL-5"));
        assertEquals("SOUTHEAST", WarrantyService.determineDealerRegion("FL-001"));
    }

    @Test
    void dealerRegionMidwest() {
        assertEquals("MIDWEST", WarrantyService.determineDealerRegion("MW-IL-3"));
        assertEquals("MIDWEST", WarrantyService.determineDealerRegion("IL-009"));
    }

    @Test
    void dealerRegionSouthwest() {
        assertEquals("SOUTHWEST", WarrantyService.determineDealerRegion("SW-TX-4"));
        assertEquals("SOUTHWEST", WarrantyService.determineDealerRegion("TX-007"));
    }

    @Test
    void dealerRegionWest() {
        assertEquals("WEST", WarrantyService.determineDealerRegion("WE-CA-2"));
        assertEquals("WEST", WarrantyService.determineDealerRegion("CA-003"));
    }

    @Test
    void dealerRegionDefaultsMidwest() {
        assertEquals("MIDWEST", WarrantyService.determineDealerRegion("ZZ-999"));
        assertEquals("MIDWEST", WarrantyService.determineDealerRegion(null));
        assertEquals("MIDWEST", WarrantyService.determineDealerRegion("X"));
    }

    @Test
    void dealerTierA() {
        assertEquals("A", WarrantyService.determineDealerTier("NE-NY-1"));
        assertEquals("A", WarrantyService.determineDealerTier("SE-FL-2"));
        assertEquals("A", WarrantyService.determineDealerTier("MW-IL-3"));
    }

    @Test
    void dealerTierB() {
        assertEquals("B", WarrantyService.determineDealerTier("SW-TX-4"));
        assertEquals("B", WarrantyService.determineDealerTier("XX-005"));
        assertEquals("B", WarrantyService.determineDealerTier("XX-006"));
    }

    @Test
    void dealerTierC() {
        assertEquals("C", WarrantyService.determineDealerTier("XX-007"));
        assertEquals("C", WarrantyService.determineDealerTier("XX-008"));
        assertEquals("C", WarrantyService.determineDealerTier("XX-00A"));
        assertEquals("C", WarrantyService.determineDealerTier(null));
        assertEquals("C", WarrantyService.determineDealerTier(""));
    }

    @Test
    void laborTypeElectrical() {
        assertEquals("ELECTRICAL", WarrantyService.determineLaborType("ECU"));
        assertEquals("ELECTRICAL", WarrantyService.determineLaborType("WIR"));
        assertEquals("ELECTRICAL", WarrantyService.determineLaborType("BAT"));
    }

    @Test
    void laborTypeMechanical() {
        assertEquals("MECHANICAL", WarrantyService.determineLaborType("ENG"));
        assertEquals("MECHANICAL", WarrantyService.determineLaborType("TRN"));
        assertEquals("MECHANICAL", WarrantyService.determineLaborType("DRV"));
        assertEquals("MECHANICAL", WarrantyService.determineLaborType("BRK"));
        assertEquals("MECHANICAL", WarrantyService.determineLaborType(null));
    }

    @Test
    void calculateMonthsBetween() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1);
        Date start = cal.getTime();
        cal.set(2024, Calendar.JULY, 1);
        Date end = cal.getTime();
        assertEquals(6, WarrantyService.calculateMonthsBetween(start, end));
    }

    @Test
    void calculateMonthsBetweenSameDate() {
        Date now = new Date();
        assertEquals(0, WarrantyService.calculateMonthsBetween(now, now));
    }

    @Test
    void calculateMonthsBetweenOneYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.MARCH, 15);
        Date start = cal.getTime();
        cal.set(2024, Calendar.MARCH, 15);
        Date end = cal.getTime();
        assertEquals(12, WarrantyService.calculateMonthsBetween(start, end));
    }
}

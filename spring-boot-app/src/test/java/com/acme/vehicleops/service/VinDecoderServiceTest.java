package com.acme.vehicleops.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VinDecoderServiceTest {

    private VinDecoderService service;

    @BeforeEach
    void setUp() {
        service = new VinDecoderService();
    }

    @Test
    void decodeNullVin() {
        Map<String, Object> result = service.decode(null);
        assertFalse((Boolean) result.get("isValid"));
    }

    @Test
    void decodeTooShort() {
        Map<String, Object> result = service.decode("1G1YY22G96");
        assertFalse((Boolean) result.get("isValid"));
    }

    @Test
    void decodeTooLong() {
        Map<String, Object> result = service.decode("1G1YY22G965110001X");
        assertFalse((Boolean) result.get("isValid"));
    }

    @Test
    void decodeExtractsWmi() {
        Map<String, Object> result = service.decode("1G1YY22G965110001");
        assertEquals("1G1", result.get("wmi"));
        assertEquals("Chevrolet (US)", result.get("manufacturer"));
    }

    @Test
    void decodeExtractsVds() {
        Map<String, Object> result = service.decode("1G1YY22G965110001");
        assertEquals("YY22G", result.get("vds"));
    }

    @Test
    void decodeExtractsModelYear() {
        Map<String, Object> result = service.decode("1G1YY22G965110001");
        assertEquals(2006, result.get("modelYear"));
    }

    @Test
    void decodeExtractsSequenceNumber() {
        Map<String, Object> result = service.decode("1G1YY22G965110001");
        assertEquals("110001", result.get("sequenceNo"));
    }

    @Test
    void decodeLowercaseConverted() {
        Map<String, Object> result = service.decode("1g1yy22g965110001");
        assertEquals("1G1", result.get("wmi"));
    }

    @Test
    void decodeUnknownManufacturer() {
        Map<String, Object> result = service.decode("ZZZYY22G965110001");
        assertTrue(((String) result.get("manufacturer")).contains("Unknown"));
    }

    @Test
    void yearCodeMapping() {
        VinDecoderService svc = new VinDecoderService();
        assertEquals(2025, svc.decode("1G1YY22G9S5110001").get("modelYear"));
        assertEquals(2026, svc.decode("1G1YY22G9T5110001").get("modelYear"));
        assertEquals(2010, svc.decode("1G1YY22G9A5110001").get("modelYear"));
        assertEquals(2001, svc.decode("1G1YY22G915110001").get("modelYear"));
    }

    @Test
    void checkDigitValidation() {
        assertTrue(service.validateCheckDigit("11111111111111111", '1'));
    }
}

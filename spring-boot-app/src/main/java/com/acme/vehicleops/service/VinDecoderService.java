package com.acme.vehicleops.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * VIN decoder — extracted from fn_DecodeVIN.
 *
 * Decodes a 17-character VIN per SAE J853 / ISO 3779:
 *   positions 1-3:  WMI (World Manufacturer Identifier)
 *   positions 4-8:  VDS (Vehicle Descriptor Section)
 *   position  9:    check digit
 *   position 10:    model year code
 *   position 11:    plant code
 *   positions 12-17: sequential production number
 */
@Service
public class VinDecoderService {

    private static final Map<String, String> WMI_MANUFACTURERS;
    private static final Map<Character, Integer> YEAR_CODE_MAP;
    private static final Map<Character, String> PLANT_CODE_MAP;
    private static final Map<Character, Integer> TRANSLITERATION;
    private static final int[] POSITIONAL_WEIGHTS = {8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2};

    static {
        Map<String, String> wmi = new HashMap<>();
        wmi.put("1FA", "Ford Motor Company");
        wmi.put("1FT", "Ford Motor Company (Trucks)");
        wmi.put("1G1", "Chevrolet (US)");
        wmi.put("1GC", "Chevrolet (Trucks)");
        wmi.put("1GM", "General Motors (Pontiac)");
        wmi.put("1N4", "Nissan (US)");
        wmi.put("2T1", "Toyota (Canada)");
        wmi.put("3FA", "Ford (Mexico)");
        wmi.put("5YJ", "Tesla Inc.");
        wmi.put("JHM", "Honda (Japan)");
        wmi.put("JTD", "Toyota (Japan)");
        wmi.put("KMH", "Hyundai (South Korea)");
        wmi.put("WAU", "Audi (Germany)");
        wmi.put("WBA", "BMW (Germany)");
        wmi.put("WDB", "Mercedes-Benz (Germany)");
        wmi.put("WVW", "Volkswagen (Germany)");
        wmi.put("ZFF", "Ferrari (Italy)");
        WMI_MANUFACTURERS = Collections.unmodifiableMap(wmi);

        Map<Character, Integer> years = new HashMap<>();
        years.put('A', 2010); years.put('B', 2011); years.put('C', 2012);
        years.put('D', 2013); years.put('E', 2014); years.put('F', 2015);
        years.put('G', 2016); years.put('H', 2017); years.put('J', 2018);
        years.put('K', 2019); years.put('L', 2020); years.put('M', 2021);
        years.put('N', 2022); years.put('P', 2023); years.put('R', 2024);
        years.put('S', 2025); years.put('T', 2026); years.put('V', 2027);
        years.put('W', 2028); years.put('X', 2029); years.put('Y', 2030);
        years.put('1', 2001); years.put('2', 2002); years.put('3', 2003);
        years.put('4', 2004); years.put('5', 2005); years.put('6', 2006);
        years.put('7', 2007); years.put('8', 2008); years.put('9', 2009);
        YEAR_CODE_MAP = Collections.unmodifiableMap(years);

        Map<Character, String> plants = new HashMap<>();
        plants.put('A', "Assembly Plant Alpha");
        plants.put('B', "Assembly Plant Bravo");
        plants.put('C', "Assembly Plant Charlie");
        plants.put('D', "Dearborn Assembly");
        plants.put('F', "Flat Rock Assembly");
        plants.put('G', "Georgetown Plant");
        plants.put('H', "Hermosillo Assembly");
        plants.put('K', "Kansas City Assembly");
        plants.put('L', "Lordstown Assembly");
        plants.put('M', "Michigan Assembly");
        plants.put('N', "Norfolk Assembly");
        plants.put('P', "Princeton Assembly");
        plants.put('R', "Arlington Assembly");
        plants.put('T', "Toledo Assembly");
        plants.put('U', "Louisville Assembly");
        plants.put('W', "Wayne Assembly");
        plants.put('X', "St. Thomas Assembly");
        PLANT_CODE_MAP = Collections.unmodifiableMap(plants);

        Map<Character, Integer> trans = new HashMap<>();
        for (char c = '0'; c <= '9'; c++) trans.put(c, c - '0');
        trans.put('A', 1); trans.put('B', 2); trans.put('C', 3);
        trans.put('D', 4); trans.put('E', 5); trans.put('F', 6);
        trans.put('G', 7); trans.put('H', 8);
        trans.put('J', 1); trans.put('K', 2); trans.put('L', 3);
        trans.put('M', 4); trans.put('N', 5);
        trans.put('P', 7); trans.put('R', 9);
        trans.put('S', 2); trans.put('T', 3); trans.put('U', 4);
        trans.put('V', 5); trans.put('W', 6); trans.put('X', 7);
        trans.put('Y', 8); trans.put('Z', 9);
        TRANSLITERATION = Collections.unmodifiableMap(trans);
    }

    public Map<String, Object> decode(String vin) {
        Map<String, Object> result = new HashMap<>();

        if (vin == null || vin.length() != 17) {
            result.put("isValid", false);
            return result;
        }

        String upperVin = vin.toUpperCase();
        String wmi = upperVin.substring(0, 3);
        String vds = upperVin.substring(3, 8);
        char checkDigit = upperVin.charAt(8);
        char yearCode = upperVin.charAt(9);
        char plantCode = upperVin.charAt(10);
        String sequenceNo = upperVin.substring(11, 17);

        String manufacturer = WMI_MANUFACTURERS.getOrDefault(wmi,
                "Unknown Manufacturer (" + wmi + ")");
        Integer modelYear = YEAR_CODE_MAP.get(yearCode);
        String plantName = PLANT_CODE_MAP.getOrDefault(plantCode,
                "Unknown Plant (" + plantCode + ")");

        boolean isValid = modelYear != null;

        if (isValid) {
            isValid = validateCheckDigit(upperVin, checkDigit);
        }

        result.put("wmi", wmi);
        result.put("manufacturer", manufacturer);
        result.put("vds", vds);
        result.put("modelYear", modelYear != null ? modelYear : "UNKNOWN");
        result.put("plantCode", String.valueOf(plantCode));
        result.put("plantName", plantName);
        result.put("sequenceNo", sequenceNo);
        result.put("isValid", isValid);

        return result;
    }

    boolean validateCheckDigit(String vin, char checkDigit) {
        int sum = 0;
        for (int pos = 0; pos < 17; pos++) {
            if (pos == 8) continue;
            char ch = vin.charAt(pos);
            Integer charVal = TRANSLITERATION.get(ch);
            if (charVal == null) charVal = 0;
            sum += charVal * POSITIONAL_WEIGHTS[pos];
        }
        int remainder = sum % 11;
        char expected = (remainder == 10) ? 'X' : (char) ('0' + remainder);
        return checkDigit == expected;
    }
}

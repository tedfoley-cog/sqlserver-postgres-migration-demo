-- ============================================================================
-- fn_DecodeVIN
-- SQL Server Scalar Function — Vehicle Identification Number Decoder
-- Vehicle Data Module
--
-- Purpose: Decodes a 17-character VIN into its component parts per the
--          SAE J853 / ISO 3779 standard. Extracts World Manufacturer
--          Identifier (WMI), Vehicle Descriptor Section (VDS), model year,
--          plant of manufacture, and sequential production number.
--
-- VIN Structure (positions 1-17):
--   1-3:  WMI  (World Manufacturer Identifier)
--   4-8:  VDS  (Vehicle Descriptor Section — model, body, engine, etc.)
--   9:    Check digit (validates VIN integrity)
--   10:   Model year code
--   11:   Plant code
--   12-17: Sequential production number
--
-- Returns: pipe-delimited string with decoded fields
--   Format: WMI|Manufacturer|VDS|ModelYear|PlantCode|PlantName|SequenceNo|IsValid
--
-- NOTE: The year-code lookup table, plant-code mapping, and check-digit
--       validation algorithm are ALL embedded in this function.
--       There is no external reference document.
--
-- History:
--   2011-09-12  G. Johnson    Created
--   2016-03-08  V. Patel      Added check digit validation
--   2019-07-22  S. Mueller    Added plant code mapping
--   2022-05-14  A. Kim        Updated year codes through 2030
-- ============================================================================

CREATE FUNCTION dbo.fn_DecodeVIN
(
    @VIN VARCHAR(17)
)
RETURNS VARCHAR(500)
AS
BEGIN
    DECLARE @Result VARCHAR(500);
    DECLARE @WMI VARCHAR(3);
    DECLARE @VDS VARCHAR(5);
    DECLARE @CheckDigit CHAR(1);
    DECLARE @YearCode CHAR(1);
    DECLARE @PlantCodeChar CHAR(1);
    DECLARE @SequenceNo VARCHAR(6);
    DECLARE @Manufacturer NVARCHAR(50);
    DECLARE @ModelYear INT;
    DECLARE @PlantName NVARCHAR(50);
    DECLARE @IsValid BIT = 1;

    -- ====================================================================
    -- Validate Length
    -- ====================================================================
    IF LEN(@VIN) != 17
    BEGIN
        SET @Result = '|||||||0';
        RETURN @Result;
    END

    -- ====================================================================
    -- Extract Components
    -- ====================================================================
    SET @WMI = SUBSTRING(@VIN, 1, 3);
    SET @VDS = SUBSTRING(@VIN, 4, 5);
    SET @CheckDigit = SUBSTRING(@VIN, 9, 1);
    SET @YearCode = SUBSTRING(@VIN, 10, 1);
    SET @PlantCodeChar = SUBSTRING(@VIN, 11, 1);
    SET @SequenceNo = SUBSTRING(@VIN, 12, 6);

    -- ====================================================================
    -- WMI → Manufacturer Lookup (common WMIs)
    -- ====================================================================
    SET @Manufacturer = CASE @WMI
        WHEN '1FA' THEN 'Ford Motor Company'
        WHEN '1FT' THEN 'Ford Motor Company (Trucks)'
        WHEN '1G1' THEN 'Chevrolet (US)'
        WHEN '1GC' THEN 'Chevrolet (Trucks)'
        WHEN '1GM' THEN 'General Motors (Pontiac)'
        WHEN '1N4' THEN 'Nissan (US)'
        WHEN '2T1' THEN 'Toyota (Canada)'
        WHEN '3FA' THEN 'Ford (Mexico)'
        WHEN '5YJ' THEN 'Tesla Inc.'
        WHEN 'JHM' THEN 'Honda (Japan)'
        WHEN 'JTD' THEN 'Toyota (Japan)'
        WHEN 'KMH' THEN 'Hyundai (South Korea)'
        WHEN 'WAU' THEN 'Audi (Germany)'
        WHEN 'WBA' THEN 'BMW (Germany)'
        WHEN 'WDB' THEN 'Mercedes-Benz (Germany)'
        WHEN 'WVW' THEN 'Volkswagen (Germany)'
        WHEN 'ZFF' THEN 'Ferrari (Italy)'
        ELSE 'Unknown Manufacturer (' + @WMI + ')'
    END;

    -- ====================================================================
    -- Year Code → Model Year (per SAE J853)
    -- ====================================================================
    SET @ModelYear = CASE @YearCode
        WHEN 'A' THEN 2010
        WHEN 'B' THEN 2011
        WHEN 'C' THEN 2012
        WHEN 'D' THEN 2013
        WHEN 'E' THEN 2014
        WHEN 'F' THEN 2015
        WHEN 'G' THEN 2016
        WHEN 'H' THEN 2017
        WHEN 'J' THEN 2018
        WHEN 'K' THEN 2019
        WHEN 'L' THEN 2020
        WHEN 'M' THEN 2021
        WHEN 'N' THEN 2022
        WHEN 'P' THEN 2023
        WHEN 'R' THEN 2024
        WHEN 'S' THEN 2025
        WHEN 'T' THEN 2026
        WHEN 'V' THEN 2027
        WHEN 'W' THEN 2028
        WHEN 'X' THEN 2029
        WHEN 'Y' THEN 2030
        WHEN '1' THEN 2001
        WHEN '2' THEN 2002
        WHEN '3' THEN 2003
        WHEN '4' THEN 2004
        WHEN '5' THEN 2005
        WHEN '6' THEN 2006
        WHEN '7' THEN 2007
        WHEN '8' THEN 2008
        WHEN '9' THEN 2009
        ELSE NULL
    END;

    IF @ModelYear IS NULL
        SET @IsValid = 0;

    -- ====================================================================
    -- Plant Code → Plant Name (manufacturer-specific mapping)
    -- Using a simplified/generic mapping for this codebase
    -- ====================================================================
    SET @PlantName = CASE @PlantCodeChar
        WHEN 'A' THEN 'Assembly Plant Alpha'
        WHEN 'B' THEN 'Assembly Plant Bravo'
        WHEN 'C' THEN 'Assembly Plant Charlie'
        WHEN 'D' THEN 'Dearborn Assembly'
        WHEN 'F' THEN 'Flat Rock Assembly'
        WHEN 'G' THEN 'Georgetown Plant'
        WHEN 'H' THEN 'Hermosillo Assembly'
        WHEN 'K' THEN 'Kansas City Assembly'
        WHEN 'L' THEN 'Lordstown Assembly'
        WHEN 'M' THEN 'Michigan Assembly'
        WHEN 'N' THEN 'Norfolk Assembly'
        WHEN 'P' THEN 'Princeton Assembly'
        WHEN 'R' THEN 'Arlington Assembly'
        WHEN 'T' THEN 'Toledo Assembly'
        WHEN 'U' THEN 'Louisville Assembly'
        WHEN 'W' THEN 'Wayne Assembly'
        WHEN 'X' THEN 'St. Thomas Assembly'
        ELSE 'Unknown Plant (' + @PlantCodeChar + ')'
    END;

    -- ====================================================================
    -- Check Digit Validation (position 9)
    -- Algorithm: transliterate letters → numbers, apply positional weights,
    -- sum products, mod 11. Result should match position 9.
    -- ====================================================================
    DECLARE @Transliteration VARCHAR(100) = '0123456789.ABCDEFGH..JKLMN.P.R..STUVWXYZ';
    DECLARE @Weights VARCHAR(17) = '8765432_098765432';  -- _ = check digit position (skipped)
    DECLARE @Sum INT = 0;
    DECLARE @Pos INT = 1;
    DECLARE @CharVal INT;
    DECLARE @Weight INT;
    DECLARE @Ch CHAR(1);

    WHILE @Pos <= 17
    BEGIN
        IF @Pos != 9  -- skip check digit position
        BEGIN
            SET @Ch = UPPER(SUBSTRING(@VIN, @Pos, 1));

            -- Transliterate: A=1, B=2, ..., H=8, J=1, K=2, ..., N=5, P=7, R=9, S=2, ...
            SET @CharVal = CASE
                WHEN @Ch BETWEEN '0' AND '9' THEN CAST(@Ch AS INT)
                WHEN @Ch = 'A' THEN 1  WHEN @Ch = 'B' THEN 2  WHEN @Ch = 'C' THEN 3
                WHEN @Ch = 'D' THEN 4  WHEN @Ch = 'E' THEN 5  WHEN @Ch = 'F' THEN 6
                WHEN @Ch = 'G' THEN 7  WHEN @Ch = 'H' THEN 8
                WHEN @Ch = 'J' THEN 1  WHEN @Ch = 'K' THEN 2  WHEN @Ch = 'L' THEN 3
                WHEN @Ch = 'M' THEN 4  WHEN @Ch = 'N' THEN 5
                WHEN @Ch = 'P' THEN 7  WHEN @Ch = 'R' THEN 9
                WHEN @Ch = 'S' THEN 2  WHEN @Ch = 'T' THEN 3  WHEN @Ch = 'U' THEN 4
                WHEN @Ch = 'V' THEN 5  WHEN @Ch = 'W' THEN 6  WHEN @Ch = 'X' THEN 7
                WHEN @Ch = 'Y' THEN 8  WHEN @Ch = 'Z' THEN 9
                ELSE 0
            END;

            -- Positional weights: 8,7,6,5,4,3,2,_,0,9,8,7,6,5,4,3,2
            SET @Weight = CASE @Pos
                WHEN 1 THEN 8   WHEN 2 THEN 7   WHEN 3 THEN 6
                WHEN 4 THEN 5   WHEN 5 THEN 4   WHEN 6 THEN 3
                WHEN 7 THEN 2   WHEN 8 THEN 10
                WHEN 10 THEN 9  WHEN 11 THEN 8  WHEN 12 THEN 7
                WHEN 13 THEN 6  WHEN 14 THEN 5  WHEN 15 THEN 4
                WHEN 16 THEN 3  WHEN 17 THEN 2
                ELSE 0
            END;

            SET @Sum = @Sum + (@CharVal * @Weight);
        END

        SET @Pos = @Pos + 1;
    END

    DECLARE @ExpectedCheck CHAR(1);
    DECLARE @Remainder INT = @Sum % 11;
    SET @ExpectedCheck = CASE
        WHEN @Remainder = 10 THEN 'X'
        ELSE CONVERT(CHAR(1), @Remainder)
    END;

    IF @CheckDigit != @ExpectedCheck
        SET @IsValid = 0;

    -- ====================================================================
    -- Assemble Result
    -- ====================================================================
    SET @Result = @WMI + '|'
        + @Manufacturer + '|'
        + @VDS + '|'
        + ISNULL(CONVERT(VARCHAR, @ModelYear), 'UNKNOWN') + '|'
        + @PlantCodeChar + '|'
        + @PlantName + '|'
        + @SequenceNo + '|'
        + CONVERT(VARCHAR, @IsValid);

    RETURN @Result;
END
GO

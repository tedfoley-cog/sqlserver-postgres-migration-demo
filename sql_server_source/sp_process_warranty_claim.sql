-- ============================================================================
-- sp_ProcessWarrantyClaim
-- SQL Server Stored Procedure — Warranty Claim Validation and Processing
-- Warranty Operations Module
--
-- Purpose: Validates and processes a warranty claim end-to-end. Checks vehicle
--          eligibility, coverage type, mileage limits, calculates costs with
--          labor rate schedules, applies deductibles, and creates or denies
--          the claim with reason codes.
--
-- NOTE: This procedure contains significant business logic that is NOT
--       documented anywhere else. The coverage rules, deductible calculations,
--       and approval thresholds are only defined here in T-SQL.
--
-- History:
--   2012-08-10  R. Santos       Created (replaced manual paper forms)
--   2014-03-22  K. Johansson    Added powertrain coverage logic
--   2016-07-15  M. Delgado      Added emissions coverage per EPA mandate
--   2018-11-02  A. Petrov       Added labor rate schedule lookup
--   2020-04-18  S. Washington   Added corrosion perforation coverage
--   2022-09-30  J. Tanaka       Added high-value claim escalation
-- ============================================================================

CREATE PROCEDURE dbo.sp_ProcessWarrantyClaim
    @VIN                VARCHAR(17),
    @DealerCode         VARCHAR(10),
    @MileageAtClaim     INT,
    @SymptomCode        VARCHAR(10),
    @CausalPartNumber   VARCHAR(25),
    @LaborOperationCode VARCHAR(15),
    @LaborHours         DECIMAL(6,2),
    @SubletCost         DECIMAL(10,2) = 0,
    @SubmittedBy        VARCHAR(50),
    -- OUTPUT
    @ClaimNumber        VARCHAR(20) OUTPUT,
    @ClaimStatus        VARCHAR(15) OUTPUT,
    @DenialReason       VARCHAR(200) OUTPUT,
    @TotalAmount        DECIMAL(10,2) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @VehicleID INT;
    DECLARE @ModelYear INT;
    DECLARE @ModelCode VARCHAR(10);
    DECLARE @WarrantyStartDate DATETIME;
    DECLARE @MonthsSinceSale INT;
    DECLARE @CoverageType VARCHAR(20);
    DECLARE @CoverageMonths INT;
    DECLARE @CoverageMiles INT;
    DECLARE @PartGroupCode VARCHAR(10);
    DECLARE @CoveredPartGroups VARCHAR(200);
    DECLARE @LaborRate DECIMAL(8,2);
    DECLARE @LaborCost DECIMAL(10,2);
    DECLARE @PartsCost DECIMAL(10,2);
    DECLARE @DeductibleAmt DECIMAL(8,2) = 0;
    DECLARE @DealerRegion VARCHAR(10);
    DECLARE @DealerTier VARCHAR(5);
    DECLARE @IsHighValue BIT = 0;

    SET @ClaimStatus = 'DENIED';
    SET @DenialReason = '';
    SET @TotalAmount = 0;

    -- ========================================================================
    -- Step 1: Validate Vehicle Exists and Get Details
    -- ========================================================================
    SELECT
        @VehicleID = VehicleID,
        @ModelYear = ModelYear,
        @ModelCode = ModelCode,
        @WarrantyStartDate = WarrantyStartDate
    FROM dbo.Vehicle WITH (NOLOCK)
    WHERE VIN = @VIN;

    IF @VehicleID IS NULL
    BEGIN
        SET @DenialReason = 'DENY_01: Vehicle not found for VIN ' + @VIN;
        RETURN;
    END

    IF @WarrantyStartDate IS NULL
    BEGIN
        SET @DenialReason = 'DENY_02: No warranty start date on file for VIN ' + @VIN;
        RETURN;
    END

    -- ========================================================================
    -- Step 2: Calculate Months Since Warranty Start
    -- ========================================================================
    SET @MonthsSinceSale = DATEDIFF(MONTH, @WarrantyStartDate, GETDATE());

    -- ========================================================================
    -- Step 3: Determine Coverage Type Based on Part Group
    -- ========================================================================
    SELECT @PartGroupCode = PartGroupCode
    FROM dbo.Part WITH (NOLOCK)
    WHERE PartNumber = @CausalPartNumber;

    IF @PartGroupCode IS NULL
    BEGIN
        SET @DenialReason = 'DENY_03: Causal part number not found: ' + @CausalPartNumber;
        RETURN;
    END

    -- Try each coverage type in priority order (most generous first)
    -- Emissions coverage: EPA-mandated, longest duration
    SELECT TOP 1
        @CoverageType = CoverageType,
        @CoverageMonths = MonthsFromSale,
        @CoverageMiles = MileageLimit,
        @CoveredPartGroups = PartGroupCodes,
        @DeductibleAmt = DeductibleAmount
    FROM dbo.WarrantyCoverage WITH (NOLOCK)
    WHERE CoverageType = 'EMISSIONS'
      AND ModelYear = @ModelYear
      AND MonthsFromSale >= @MonthsSinceSale
      AND MileageLimit >= @MileageAtClaim
      AND CHARINDEX(@PartGroupCode, PartGroupCodes) > 0
    ORDER BY MonthsFromSale DESC;

    -- Powertrain coverage
    IF @CoverageType IS NULL
    BEGIN
        SELECT TOP 1
            @CoverageType = CoverageType,
            @CoverageMonths = MonthsFromSale,
            @CoverageMiles = MileageLimit,
            @CoveredPartGroups = PartGroupCodes,
            @DeductibleAmt = DeductibleAmount
        FROM dbo.WarrantyCoverage WITH (NOLOCK)
        WHERE CoverageType = 'POWERTRAIN'
          AND ModelYear = @ModelYear
          AND MonthsFromSale >= @MonthsSinceSale
          AND MileageLimit >= @MileageAtClaim
          AND CHARINDEX(@PartGroupCode, PartGroupCodes) > 0
        ORDER BY MonthsFromSale DESC;
    END

    -- Corrosion perforation coverage
    IF @CoverageType IS NULL
    BEGIN
        SELECT TOP 1
            @CoverageType = CoverageType,
            @CoverageMonths = MonthsFromSale,
            @CoverageMiles = MileageLimit,
            @CoveredPartGroups = PartGroupCodes,
            @DeductibleAmt = DeductibleAmount
        FROM dbo.WarrantyCoverage WITH (NOLOCK)
        WHERE CoverageType = 'CORROSION'
          AND ModelYear = @ModelYear
          AND MonthsFromSale >= @MonthsSinceSale
          AND MileageLimit >= @MileageAtClaim
          AND CHARINDEX(@PartGroupCode, PartGroupCodes) > 0
        ORDER BY MonthsFromSale DESC;
    END

    -- Bumper-to-bumper (basic) coverage
    IF @CoverageType IS NULL
    BEGIN
        SELECT TOP 1
            @CoverageType = CoverageType,
            @CoverageMonths = MonthsFromSale,
            @CoverageMiles = MileageLimit,
            @CoveredPartGroups = PartGroupCodes,
            @DeductibleAmt = DeductibleAmount
        FROM dbo.WarrantyCoverage WITH (NOLOCK)
        WHERE CoverageType = 'BUMPER_TO_BUMPER'
          AND ModelYear = @ModelYear
          AND MonthsFromSale >= @MonthsSinceSale
          AND MileageLimit >= @MileageAtClaim
          AND (PartGroupCodes IS NULL OR CHARINDEX(@PartGroupCode, PartGroupCodes) > 0)
        ORDER BY MonthsFromSale DESC;
    END

    IF @CoverageType IS NULL
    BEGIN
        SET @DenialReason = 'DENY_04: No applicable coverage for part group '
            + @PartGroupCode + ' at ' + CONVERT(VARCHAR, @MonthsSinceSale)
            + ' months / ' + CONVERT(VARCHAR, @MileageAtClaim) + ' miles';
        RETURN;
    END

    -- ========================================================================
    -- Step 4: Look Up Labor Rate for Dealer Region and Tier
    -- ========================================================================
    -- Determine dealer region and tier (hardcoded mapping — undocumented)
    SET @DealerRegion = CASE
        WHEN LEFT(@DealerCode, 2) IN ('NE', 'NY', 'CT', 'MA') THEN 'NORTHEAST'
        WHEN LEFT(@DealerCode, 2) IN ('SE', 'FL', 'GA', 'NC') THEN 'SOUTHEAST'
        WHEN LEFT(@DealerCode, 2) IN ('MW', 'IL', 'OH', 'MI') THEN 'MIDWEST'
        WHEN LEFT(@DealerCode, 2) IN ('SW', 'TX', 'AZ', 'NM') THEN 'SOUTHWEST'
        WHEN LEFT(@DealerCode, 2) IN ('WE', 'CA', 'WA', 'OR') THEN 'WEST'
        ELSE 'MIDWEST'  -- default
    END;

    SET @DealerTier = CASE
        WHEN RIGHT(@DealerCode, 1) IN ('1', '2', '3') THEN 'A'
        WHEN RIGHT(@DealerCode, 1) IN ('4', '5', '6') THEN 'B'
        ELSE 'C'
    END;

    -- Get the labor rate
    SELECT TOP 1 @LaborRate = HourlyRate
    FROM dbo.LaborRateSchedule WITH (NOLOCK)
    WHERE RegionCode = @DealerRegion
      AND DealerTier = @DealerTier
      AND LaborType = CASE
            WHEN @PartGroupCode IN ('ENG', 'TRN', 'DRV') THEN 'MECHANICAL'
            WHEN @PartGroupCode IN ('ECU', 'WIR', 'BAT') THEN 'ELECTRICAL'
            ELSE 'MECHANICAL'
          END
      AND EffectiveDate <= GETDATE()
      AND (ExpirationDate IS NULL OR ExpirationDate > GETDATE())
    ORDER BY EffectiveDate DESC;

    IF @LaborRate IS NULL
        SET @LaborRate = 85.00;  -- fallback default rate

    -- ========================================================================
    -- Step 5: Calculate Costs
    -- ========================================================================
    SET @LaborCost = @LaborHours * @LaborRate;

    SELECT @PartsCost = ISNULL(UnitCost, 0)
    FROM dbo.Part WITH (NOLOCK)
    WHERE PartNumber = @CausalPartNumber;

    SET @TotalAmount = @LaborCost + ISNULL(@PartsCost, 0) + @SubletCost - @DeductibleAmt;

    IF @TotalAmount < 0
        SET @TotalAmount = 0;

    -- ========================================================================
    -- Step 6: High-Value Claim Check (>$2500 requires escalation flag)
    -- ========================================================================
    IF @TotalAmount > 2500.00
        SET @IsHighValue = 1;

    -- ========================================================================
    -- Step 7: Generate Claim Number and Insert
    -- ========================================================================
    SET @ClaimNumber = 'WC-' + FORMAT(GETDATE(), 'yyyyMMdd') + '-'
        + RIGHT('0000' + CONVERT(VARCHAR, (
            SELECT ISNULL(MAX(ClaimID), 0) + 1 FROM dbo.WarrantyClaim
          )), 4);

    SET @ClaimStatus = CASE
        WHEN @IsHighValue = 1 THEN 'SUBMITTED'   -- needs manual review
        ELSE 'APPROVED'
    END;

    INSERT INTO dbo.WarrantyClaim (
        ClaimNumber, VehicleID, DealerCode, ClaimDate, MileageAtClaim,
        SymptomCode, CausalPartNumber, LaborOperationCode, LaborHours,
        LaborRate, PartsCost, SubletCost, Deductible, TotalAmount,
        CoverageType, ClaimStatus, ApprovedDate, SubmittedBy
    )
    VALUES (
        @ClaimNumber, @VehicleID, @DealerCode, GETDATE(), @MileageAtClaim,
        @SymptomCode, @CausalPartNumber, @LaborOperationCode, @LaborHours,
        @LaborRate, @PartsCost, @SubletCost, @DeductibleAmt, @TotalAmount,
        @CoverageType, @ClaimStatus,
        CASE WHEN @ClaimStatus = 'APPROVED' THEN GETDATE() ELSE NULL END,
        @SubmittedBy
    );

    -- ========================================================================
    -- Step 8: Update Vehicle Mileage
    -- ========================================================================
    UPDATE dbo.Vehicle
    SET CurrentMileage = @MileageAtClaim,
        ModifiedDate = GETDATE()
    WHERE VehicleID = @VehicleID
      AND CurrentMileage < @MileageAtClaim;

END
GO

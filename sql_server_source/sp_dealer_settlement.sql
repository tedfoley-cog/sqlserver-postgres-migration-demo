-- ============================================================================
-- sp_DealerSettlement
-- SQL Server Stored Procedure — Dealer Warranty Reimbursement Batch
-- Finance Module
--
-- Purpose: Processes a batch of approved warranty claims into dealer
--          settlement records. Calculates reimbursement based on regional
--          labor rate schedules, parts markup rules, and sublet allowances.
--          Groups claims by dealer for batch payment.
--
-- Parts Markup Rules (undocumented — embedded here):
--   OEM parts:        40% markup on cost
--   Remanufactured:   25% markup on cost
--   Aftermarket:      15% markup on cost (only with prior auth)
--   List price cap:   markup cannot exceed list price
--
-- NOTE: Settlement logic, markup rules, and batch numbering scheme
--       are only defined in this procedure. No external documentation.
--
-- History:
--   2014-02-14  P. O'Brien      Created for monthly settlement runs
--   2016-06-22  R. Gupta        Added parts markup tiers
--   2018-10-05  D. Kowalczyk    Added sublet cost allowance cap
--   2020-07-18  J. Williams     Added batch settlement grouping
--   2023-01-09  M. Petrova      Added adjustment memo support
-- ============================================================================

CREATE PROCEDURE dbo.sp_DealerSettlement
    @DealerCode         VARCHAR(10) = NULL,  -- NULL = all dealers with approved claims
    @CutoffDate         DATETIME = NULL,     -- NULL = all approved claims
    -- OUTPUT
    @BatchNumber        VARCHAR(20) OUTPUT,
    @TotalClaimsSettled INT OUTPUT,
    @TotalBatchAmount   DECIMAL(12,2) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @CutoffDate IS NULL
        SET @CutoffDate = GETDATE();

    -- ========================================================================
    -- Step 1: Generate Batch Number
    -- ========================================================================
    SET @BatchNumber = 'STL-' + FORMAT(@CutoffDate, 'yyyyMMdd') + '-'
        + RIGHT('000' + CONVERT(VARCHAR,
            (SELECT ISNULL(COUNT(*), 0) + 1
             FROM dbo.DealerSettlement WITH (NOLOCK)
             WHERE SettlementBatchNo LIKE 'STL-' + FORMAT(@CutoffDate, 'yyyyMMdd') + '%')
          ), 3);

    SET @TotalClaimsSettled = 0;
    SET @TotalBatchAmount = 0;

    -- ========================================================================
    -- Step 2: Gather approved claims not yet settled
    -- ========================================================================
    CREATE TABLE #ClaimsToSettle (
        ClaimID             INT,
        ClaimNumber         VARCHAR(20),
        DealerCode          VARCHAR(10),
        LaborHours          DECIMAL(6,2),
        LaborRate           DECIMAL(8,2),
        LaborAmount         DECIMAL(10,2),
        CausalPartNumber    VARCHAR(25),
        PartsCost           DECIMAL(10,2),
        PartsMarkupPct      DECIMAL(5,2),
        PartsMarkupAmount   DECIMAL(10,2),
        PartsTotal          DECIMAL(10,2),
        SubletCost          DECIMAL(10,2),
        SubletAllowed       DECIMAL(10,2),
        Deductible          DECIMAL(8,2),
        NetAmount           DECIMAL(10,2)
    );

    INSERT INTO #ClaimsToSettle (
        ClaimID, ClaimNumber, DealerCode, LaborHours, LaborRate, LaborAmount,
        CausalPartNumber, PartsCost, PartsMarkupPct, SubletCost, Deductible
    )
    SELECT
        wc.ClaimID,
        wc.ClaimNumber,
        wc.DealerCode,
        wc.LaborHours,
        wc.LaborRate,
        wc.LaborHours * wc.LaborRate,       -- labor amount
        wc.CausalPartNumber,
        ISNULL(wc.PartsCost, 0),
        -- Parts markup % based on part supplier type (undocumented rule)
        CASE
            WHEN p.SupplierCode LIKE 'OEM%' THEN 40.00
            WHEN p.SupplierCode LIKE 'REMAN%' THEN 25.00
            WHEN p.SupplierCode LIKE 'AFT%' THEN 15.00
            ELSE 30.00   -- default
        END,
        ISNULL(wc.SubletCost, 0),
        ISNULL(wc.Deductible, 0)
    FROM dbo.WarrantyClaim wc WITH (NOLOCK)
    LEFT JOIN dbo.Part p WITH (NOLOCK)
        ON p.PartNumber = wc.CausalPartNumber
    WHERE wc.ClaimStatus = 'APPROVED'
      AND wc.SettledDate IS NULL
      AND wc.ApprovedDate <= @CutoffDate
      AND (@DealerCode IS NULL OR wc.DealerCode = @DealerCode);

    -- ========================================================================
    -- Step 3: Calculate Parts Markup (capped at list price)
    -- ========================================================================
    UPDATE cts
    SET PartsMarkupAmount = CASE
            WHEN cts.PartsCost = 0 THEN 0
            WHEN (cts.PartsCost * (1 + cts.PartsMarkupPct / 100)) > ISNULL(p.ListPrice, 99999)
                THEN ISNULL(p.ListPrice, cts.PartsCost) - cts.PartsCost
            ELSE ROUND(cts.PartsCost * cts.PartsMarkupPct / 100, 2)
        END,
        PartsTotal = CASE
            WHEN cts.PartsCost = 0 THEN 0
            WHEN (cts.PartsCost * (1 + cts.PartsMarkupPct / 100)) > ISNULL(p.ListPrice, 99999)
                THEN ISNULL(p.ListPrice, cts.PartsCost)
            ELSE ROUND(cts.PartsCost * (1 + cts.PartsMarkupPct / 100), 2)
        END
    FROM #ClaimsToSettle cts
    LEFT JOIN dbo.Part p WITH (NOLOCK)
        ON p.PartNumber = cts.CausalPartNumber;

    -- ========================================================================
    -- Step 4: Apply Sublet Cost Allowance Cap
    --         Sublet costs capped at 80% of submitted amount (undocumented)
    -- ========================================================================
    UPDATE #ClaimsToSettle
    SET SubletAllowed = CASE
        WHEN SubletCost <= 500 THEN SubletCost           -- small sublets pass through
        ELSE ROUND(SubletCost * 0.80, 2)                 -- 80% cap for larger sublets
    END;

    -- ========================================================================
    -- Step 5: Calculate Net Amount Per Claim
    -- ========================================================================
    UPDATE #ClaimsToSettle
    SET NetAmount = LaborAmount
        + ISNULL(PartsTotal, 0)
        + ISNULL(SubletAllowed, 0)
        - ISNULL(Deductible, 0);

    -- Floor at zero
    UPDATE #ClaimsToSettle
    SET NetAmount = 0
    WHERE NetAmount < 0;

    -- ========================================================================
    -- Step 6: Insert Settlement Records
    -- ========================================================================
    BEGIN TRANSACTION;

    INSERT INTO dbo.DealerSettlement (
        SettlementBatchNo, DealerCode, ClaimID,
        LaborAmount, PartsAmount, PartsMarkup,
        SubletAmount, DeductibleCredit, NetAmount,
        SettlementDate, PaymentStatus
    )
    SELECT
        @BatchNumber,
        DealerCode,
        ClaimID,
        LaborAmount,
        PartsCost,
        PartsMarkupAmount,
        SubletAllowed,
        Deductible,
        NetAmount,
        @CutoffDate,
        'PENDING'
    FROM #ClaimsToSettle;

    SET @TotalClaimsSettled = @@ROWCOUNT;

    -- ========================================================================
    -- Step 7: Update Warranty Claims as Settled
    -- ========================================================================
    UPDATE wc
    SET wc.ClaimStatus = 'SETTLED',
        wc.SettledDate = @CutoffDate
    FROM dbo.WarrantyClaim wc
    INNER JOIN #ClaimsToSettle cts
        ON cts.ClaimID = wc.ClaimID;

    COMMIT TRANSACTION;

    -- ========================================================================
    -- Step 8: Return Batch Summary
    -- ========================================================================
    SET @TotalBatchAmount = (SELECT ISNULL(SUM(NetAmount), 0) FROM #ClaimsToSettle);

    -- Result set 1: Settlement detail
    SELECT * FROM #ClaimsToSettle ORDER BY DealerCode, ClaimNumber;

    -- Result set 2: Dealer-level summary
    SELECT
        DealerCode,
        COUNT(*) AS ClaimCount,
        SUM(LaborAmount) AS TotalLabor,
        SUM(PartsTotal) AS TotalParts,
        SUM(PartsMarkupAmount) AS TotalMarkup,
        SUM(SubletAllowed) AS TotalSublet,
        SUM(Deductible) AS TotalDeductibles,
        SUM(NetAmount) AS NetPayable
    FROM #ClaimsToSettle
    GROUP BY DealerCode
    ORDER BY NetPayable DESC;

    DROP TABLE #ClaimsToSettle;
END
GO

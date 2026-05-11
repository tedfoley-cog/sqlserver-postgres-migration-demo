-- ============================================================================
-- sp_SupplierQualityScorecard
-- SQL Server Stored Procedure — Supplier Quality Performance Scorecard
-- Supplier Quality Module
--
-- Purpose: Calculates a weighted quality scorecard for suppliers over a
--          rolling period. Aggregates PPM (parts per million defects),
--          on-time delivery rate, rejection rate, and certification status
--          into a composite score with red/yellow/green rating.
--
-- Scoring Weights (hardcoded — undocumented):
--   PPM:              30%
--   On-Time Delivery: 30%
--   Rejection Rate:   25%
--   Certification:    15%
--
-- NOTE: The scoring formula, threshold values, and rating boundaries
--       are only defined in this stored procedure.
--
-- History:
--   2015-03-20  A. Kowalski     Created for quarterly supplier reviews
--   2017-08-14  M. Singh        Added rolling 12-month calculation
--   2019-05-30  L. Dubois       Added certification status weighting
--   2021-12-07  T. Hassan       Added trend analysis (improving/declining)
-- ============================================================================

CREATE PROCEDURE dbo.sp_SupplierQualityScorecard
    @SupplierCode       VARCHAR(20) = NULL,   -- NULL = all suppliers
    @RollingMonths      INT = 12,
    @MinShipments       INT = 5,              -- minimum shipments to score
    -- OUTPUT
    @TotalSuppliersScored INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @CutoffDate DATETIME = DATEADD(MONTH, -@RollingMonths, GETDATE());
    DECLARE @HalfwayDate DATETIME = DATEADD(MONTH, -(@RollingMonths / 2), GETDATE());

    -- Scoring weights
    DECLARE @WeightPPM DECIMAL(3,2) = 0.30;
    DECLARE @WeightOTD DECIMAL(3,2) = 0.30;
    DECLARE @WeightRejection DECIMAL(3,2) = 0.25;
    DECLARE @WeightCert DECIMAL(3,2) = 0.15;

    -- ========================================================================
    -- Step 1: Aggregate shipment metrics per supplier
    -- ========================================================================
    CREATE TABLE #SupplierMetrics (
        SupplierCode        VARCHAR(20),
        SupplierName        NVARCHAR(100),
        Region              VARCHAR(10),
        TotalShipments      INT,
        TotalOrdered        INT,
        TotalReceived       INT,
        TotalRejected       INT,
        OnTimeShipments     INT,
        PPMDefects          DECIMAL(10,2),
        OnTimeDeliveryPct   DECIMAL(5,2),
        RejectionRatePct    DECIMAL(5,2),
        CertStatus          VARCHAR(10),         -- 'VALID', 'EXPIRING', 'EXPIRED', 'NONE'
        CertDaysRemaining   INT,
        -- First half / second half for trend
        PPM_FirstHalf       DECIMAL(10,2),
        PPM_SecondHalf      DECIMAL(10,2),
        OTD_FirstHalf       DECIMAL(5,2),
        OTD_SecondHalf      DECIMAL(5,2),
        -- Scores (0-100)
        PPMScore            DECIMAL(5,2),
        OTDScore            DECIMAL(5,2),
        RejectionScore      DECIMAL(5,2),
        CertScore           DECIMAL(5,2),
        CompositeScore      DECIMAL(5,2),
        Rating              VARCHAR(10),
        Trend               VARCHAR(15)
    );

    INSERT INTO #SupplierMetrics (
        SupplierCode, SupplierName, Region,
        TotalShipments, TotalOrdered, TotalReceived, TotalRejected,
        OnTimeShipments, CertStatus, CertDaysRemaining,
        PPMDefects, OnTimeDeliveryPct, RejectionRatePct,
        PPM_FirstHalf, PPM_SecondHalf, OTD_FirstHalf, OTD_SecondHalf
    )
    SELECT
        s.SupplierCode,
        s.SupplierName,
        s.Region,
        COUNT(sh.ShipmentID),
        ISNULL(SUM(sh.OrderedQty), 0),
        ISNULL(SUM(sh.ReceivedQty), 0),
        ISNULL(SUM(sh.RejectedQty), 0),
        ISNULL(SUM(CASE WHEN sh.IsOnTime = 1 THEN 1 ELSE 0 END), 0),
        -- Certification status
        CASE
            WHEN s.CertificationExpiry IS NULL THEN 'NONE'
            WHEN s.CertificationExpiry < GETDATE() THEN 'EXPIRED'
            WHEN DATEDIFF(DAY, GETDATE(), s.CertificationExpiry) <= 90 THEN 'EXPIRING'
            ELSE 'VALID'
        END,
        ISNULL(DATEDIFF(DAY, GETDATE(), s.CertificationExpiry), -999),
        -- PPM = (rejected / received) * 1,000,000
        CASE
            WHEN ISNULL(SUM(sh.ReceivedQty), 0) = 0 THEN 0
            ELSE ROUND(CAST(ISNULL(SUM(sh.RejectedQty), 0) AS DECIMAL)
                 / CAST(SUM(sh.ReceivedQty) AS DECIMAL) * 1000000, 2)
        END,
        -- On-Time Delivery %
        CASE
            WHEN COUNT(sh.ShipmentID) = 0 THEN 0
            ELSE ROUND(CAST(SUM(CASE WHEN sh.IsOnTime = 1 THEN 1 ELSE 0 END) AS DECIMAL)
                 / CAST(COUNT(sh.ShipmentID) AS DECIMAL) * 100, 2)
        END,
        -- Rejection Rate %
        CASE
            WHEN ISNULL(SUM(sh.ReceivedQty), 0) = 0 THEN 0
            ELSE ROUND(CAST(ISNULL(SUM(sh.RejectedQty), 0) AS DECIMAL)
                 / CAST(SUM(sh.ReceivedQty) AS DECIMAL) * 100, 2)
        END,
        -- PPM first half
        CASE
            WHEN (SELECT ISNULL(SUM(sh2.ReceivedQty), 0) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                  WHERE sh2.SupplierCode = s.SupplierCode
                    AND sh2.ShipDate >= @CutoffDate AND sh2.ShipDate < @HalfwayDate) = 0 THEN 0
            ELSE ROUND(
                CAST((SELECT ISNULL(SUM(sh2.RejectedQty), 0) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                      WHERE sh2.SupplierCode = s.SupplierCode
                        AND sh2.ShipDate >= @CutoffDate AND sh2.ShipDate < @HalfwayDate) AS DECIMAL)
                / CAST((SELECT SUM(sh2.ReceivedQty) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                        WHERE sh2.SupplierCode = s.SupplierCode
                          AND sh2.ShipDate >= @CutoffDate AND sh2.ShipDate < @HalfwayDate) AS DECIMAL)
                * 1000000, 2)
        END,
        -- PPM second half
        CASE
            WHEN (SELECT ISNULL(SUM(sh2.ReceivedQty), 0) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                  WHERE sh2.SupplierCode = s.SupplierCode
                    AND sh2.ShipDate >= @HalfwayDate) = 0 THEN 0
            ELSE ROUND(
                CAST((SELECT ISNULL(SUM(sh2.RejectedQty), 0) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                      WHERE sh2.SupplierCode = s.SupplierCode
                        AND sh2.ShipDate >= @HalfwayDate) AS DECIMAL)
                / CAST((SELECT SUM(sh2.ReceivedQty) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                        WHERE sh2.SupplierCode = s.SupplierCode
                          AND sh2.ShipDate >= @HalfwayDate) AS DECIMAL)
                * 1000000, 2)
        END,
        -- OTD first half
        CASE
            WHEN (SELECT COUNT(*) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                  WHERE sh2.SupplierCode = s.SupplierCode
                    AND sh2.ShipDate >= @CutoffDate AND sh2.ShipDate < @HalfwayDate) = 0 THEN 0
            ELSE ROUND(
                CAST((SELECT SUM(CASE WHEN sh2.IsOnTime = 1 THEN 1 ELSE 0 END)
                      FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                      WHERE sh2.SupplierCode = s.SupplierCode
                        AND sh2.ShipDate >= @CutoffDate AND sh2.ShipDate < @HalfwayDate) AS DECIMAL)
                / CAST((SELECT COUNT(*) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                        WHERE sh2.SupplierCode = s.SupplierCode
                          AND sh2.ShipDate >= @CutoffDate AND sh2.ShipDate < @HalfwayDate) AS DECIMAL)
                * 100, 2)
        END,
        -- OTD second half
        CASE
            WHEN (SELECT COUNT(*) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                  WHERE sh2.SupplierCode = s.SupplierCode
                    AND sh2.ShipDate >= @HalfwayDate) = 0 THEN 0
            ELSE ROUND(
                CAST((SELECT SUM(CASE WHEN sh2.IsOnTime = 1 THEN 1 ELSE 0 END)
                      FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                      WHERE sh2.SupplierCode = s.SupplierCode
                        AND sh2.ShipDate >= @HalfwayDate) AS DECIMAL)
                / CAST((SELECT COUNT(*) FROM dbo.SupplierShipment sh2 WITH (NOLOCK)
                        WHERE sh2.SupplierCode = s.SupplierCode
                          AND sh2.ShipDate >= @HalfwayDate) AS DECIMAL)
                * 100, 2)
        END
    FROM dbo.Supplier s WITH (NOLOCK)
    LEFT JOIN dbo.SupplierShipment sh WITH (NOLOCK)
        ON sh.SupplierCode = s.SupplierCode
        AND sh.ShipDate >= @CutoffDate
    WHERE (@SupplierCode IS NULL OR s.SupplierCode = @SupplierCode)
    GROUP BY s.SupplierCode, s.SupplierName, s.Region,
             s.CertificationExpiry, s.IsApproved
    HAVING COUNT(sh.ShipmentID) >= @MinShipments;

    -- ========================================================================
    -- Step 2: Calculate Individual Scores (0-100 scale)
    -- ========================================================================

    -- PPM Score: 0 PPM = 100, 500 PPM = 80, 1000 PPM = 60, 5000+ = 0
    UPDATE #SupplierMetrics
    SET PPMScore = CASE
        WHEN PPMDefects = 0 THEN 100
        WHEN PPMDefects <= 100 THEN 95
        WHEN PPMDefects <= 500 THEN 80
        WHEN PPMDefects <= 1000 THEN 60
        WHEN PPMDefects <= 2500 THEN 40
        WHEN PPMDefects <= 5000 THEN 20
        ELSE 0
    END;

    -- OTD Score: direct mapping (98% OTD = 98 score)
    UPDATE #SupplierMetrics
    SET OTDScore = OnTimeDeliveryPct;

    -- Rejection Score: inverse of rejection rate
    UPDATE #SupplierMetrics
    SET RejectionScore = CASE
        WHEN RejectionRatePct = 0 THEN 100
        WHEN RejectionRatePct <= 0.5 THEN 90
        WHEN RejectionRatePct <= 1.0 THEN 75
        WHEN RejectionRatePct <= 2.0 THEN 60
        WHEN RejectionRatePct <= 5.0 THEN 40
        ELSE 20
    END;

    -- Certification Score
    UPDATE #SupplierMetrics
    SET CertScore = CASE
        WHEN CertStatus = 'VALID' THEN 100
        WHEN CertStatus = 'EXPIRING' THEN 70
        WHEN CertStatus = 'EXPIRED' THEN 20
        ELSE 0   -- NONE
    END;

    -- ========================================================================
    -- Step 3: Calculate Composite Score and Rating
    -- ========================================================================
    UPDATE #SupplierMetrics
    SET CompositeScore = ROUND(
        (PPMScore * @WeightPPM)
        + (OTDScore * @WeightOTD)
        + (RejectionScore * @WeightRejection)
        + (CertScore * @WeightCert), 2);

    UPDATE #SupplierMetrics
    SET Rating = CASE
        WHEN CompositeScore >= 90 THEN 'GREEN'
        WHEN CompositeScore >= 70 THEN 'YELLOW'
        ELSE 'RED'
    END;

    -- ========================================================================
    -- Step 4: Determine Trend (improving / stable / declining)
    -- ========================================================================
    UPDATE #SupplierMetrics
    SET Trend = CASE
        WHEN PPM_SecondHalf < PPM_FirstHalf * 0.9
             AND OTD_SecondHalf > OTD_FirstHalf * 1.02
            THEN 'IMPROVING'
        WHEN PPM_SecondHalf > PPM_FirstHalf * 1.1
             OR OTD_SecondHalf < OTD_FirstHalf * 0.95
            THEN 'DECLINING'
        ELSE 'STABLE'
    END;

    -- ========================================================================
    -- Step 5: Return Results
    -- ========================================================================
    SET @TotalSuppliersScored = (SELECT COUNT(*) FROM #SupplierMetrics);

    SELECT
        SupplierCode, SupplierName, Region,
        TotalShipments, TotalOrdered, TotalReceived, TotalRejected,
        PPMDefects, OnTimeDeliveryPct, RejectionRatePct,
        CertStatus, CertDaysRemaining,
        PPMScore, OTDScore, RejectionScore, CertScore,
        CompositeScore, Rating, Trend
    FROM #SupplierMetrics
    ORDER BY CompositeScore DESC;

    DROP TABLE #SupplierMetrics;
END
GO

-- ============================================================================
-- sp_PartsSupersession
-- SQL Server Stored Procedure — Parts Supersession Chain Lookup
-- Parts Catalog Module
--
-- Purpose: Walks the parts supersession chain to find the current active
--          replacement for a given part number. Validates fitment against
--          a specific vehicle (by VIN or model/year). Returns the full
--          chain with pricing and availability.
--
-- Supersession Example:
--   PT-1001 → PT-1001A → PT-1001B (current)
--   A dealer searching for PT-1001 should be shown PT-1001B.
--
-- NOTE: The supersession chain walk, fitment validation, and cross-reference
--       logic are all embedded here. No application-layer documentation.
--
-- History:
--   2013-01-18  L. Fernandez    Created
--   2015-08-09  J. Olsen        Added fitment validation by VIN
--   2018-04-22  B. Nakamura     Added cross-reference (aftermarket) lookup
--   2020-11-15  K. Moreno       Added inventory availability check
--   2023-03-10  R. Ivanova      Added depth limit to prevent infinite loops
-- ============================================================================

CREATE PROCEDURE dbo.sp_PartsSupersession
    @OriginalPartNumber VARCHAR(25),
    @VIN                VARCHAR(17) = NULL,    -- optional: validate fitment
    @ModelCode          VARCHAR(10) = NULL,    -- alternative to VIN
    @ModelYear          INT = NULL,            -- alternative to VIN
    @IncludeInventory   BIT = 1,
    @MaxChainDepth      INT = 10               -- prevent infinite loops
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @CurrentPartNo VARCHAR(25);
    DECLARE @NextPartNo VARCHAR(25);
    DECLARE @Depth INT = 0;
    DECLARE @VehicleModelCode VARCHAR(10);
    DECLARE @VehicleModelYear INT;
    DECLARE @VehicleTrimLevel VARCHAR(20);
    DECLARE @VehicleEngineCode VARCHAR(15);

    -- ========================================================================
    -- Step 1: Resolve vehicle details if VIN provided
    -- ========================================================================
    IF @VIN IS NOT NULL
    BEGIN
        SELECT
            @VehicleModelCode = ModelCode,
            @VehicleModelYear = ModelYear,
            @VehicleTrimLevel = TrimLevel,
            @VehicleEngineCode = EngineCode
        FROM dbo.Vehicle WITH (NOLOCK)
        WHERE VIN = @VIN;

        IF @VehicleModelCode IS NULL
        BEGIN
            RAISERROR('Vehicle not found for VIN: %s', 16, 1, @VIN);
            RETURN;
        END
    END
    ELSE
    BEGIN
        SET @VehicleModelCode = @ModelCode;
        SET @VehicleModelYear = @ModelYear;
    END

    -- ========================================================================
    -- Step 2: Build the supersession chain using recursive walk
    -- ========================================================================
    CREATE TABLE #SupersessionChain (
        ChainPosition       INT,
        PartNumber          VARCHAR(25),
        Description         NVARCHAR(100),
        PartGroupCode       VARCHAR(10),
        UnitCost            DECIMAL(10,2),
        ListPrice           DECIMAL(10,2),
        Status              VARCHAR(15),
        SupplierCode        VARCHAR(20),
        IsOriginalRequest   BIT DEFAULT 0,
        IsCurrentActive     BIT DEFAULT 0
    );

    SET @CurrentPartNo = @OriginalPartNumber;

    WHILE @CurrentPartNo IS NOT NULL AND @Depth < @MaxChainDepth
    BEGIN
        INSERT INTO #SupersessionChain (
            ChainPosition, PartNumber, Description, PartGroupCode,
            UnitCost, ListPrice, Status, SupplierCode, IsOriginalRequest
        )
        SELECT
            @Depth,
            p.PartNumber,
            p.Description,
            p.PartGroupCode,
            p.UnitCost,
            p.ListPrice,
            p.Status,
            p.SupplierCode,
            CASE WHEN @Depth = 0 THEN 1 ELSE 0 END
        FROM dbo.Part p WITH (NOLOCK)
        WHERE p.PartNumber = @CurrentPartNo;

        -- Check for next in chain
        SELECT @NextPartNo = SupersededByPartNo
        FROM dbo.Part WITH (NOLOCK)
        WHERE PartNumber = @CurrentPartNo
          AND SupersededByPartNo IS NOT NULL
          AND Status = 'SUPERSEDED';

        -- Self-reference guard
        IF @NextPartNo = @CurrentPartNo
            SET @NextPartNo = NULL;

        -- Already-visited guard
        IF EXISTS (SELECT 1 FROM #SupersessionChain WHERE PartNumber = @NextPartNo)
            SET @NextPartNo = NULL;

        SET @CurrentPartNo = @NextPartNo;
        SET @Depth = @Depth + 1;
    END

    -- Mark the last active part in the chain
    UPDATE #SupersessionChain
    SET IsCurrentActive = 1
    WHERE ChainPosition = (SELECT MAX(ChainPosition) FROM #SupersessionChain)
      AND Status = 'ACTIVE';

    -- ========================================================================
    -- Step 3: Validate Fitment for Each Part in Chain
    -- ========================================================================
    CREATE TABLE #FitmentResults (
        PartNumber          VARCHAR(25),
        FitsVehicle         BIT DEFAULT 0,
        FitmentNotes        NVARCHAR(200)
    );

    IF @VehicleModelCode IS NOT NULL AND @VehicleModelYear IS NOT NULL
    BEGIN
        INSERT INTO #FitmentResults (PartNumber, FitsVehicle, FitmentNotes)
        SELECT
            sc.PartNumber,
            CASE
                WHEN pf.FitmentID IS NOT NULL
                    AND @VehicleModelYear BETWEEN pf.ModelYearFrom AND pf.ModelYearTo
                    AND (pf.TrimLevel IS NULL OR pf.TrimLevel = @VehicleTrimLevel)
                    AND (pf.EngineCode IS NULL OR pf.EngineCode = @VehicleEngineCode)
                THEN 1
                ELSE 0
            END,
            CASE
                WHEN pf.FitmentID IS NULL THEN 'No fitment data on file'
                WHEN @VehicleModelYear < pf.ModelYearFrom THEN 'Model year too early'
                WHEN @VehicleModelYear > pf.ModelYearTo THEN 'Model year too late'
                WHEN pf.TrimLevel IS NOT NULL AND pf.TrimLevel != @VehicleTrimLevel
                    THEN 'Trim mismatch (requires ' + pf.TrimLevel + ')'
                WHEN pf.EngineCode IS NOT NULL AND pf.EngineCode != @VehicleEngineCode
                    THEN 'Engine mismatch (requires ' + pf.EngineCode + ')'
                ELSE pf.Notes
            END
        FROM #SupersessionChain sc
        LEFT JOIN dbo.PartFitment pf WITH (NOLOCK)
            ON pf.PartNumber = sc.PartNumber
            AND pf.ModelCode = @VehicleModelCode;
    END

    -- ========================================================================
    -- Step 4: Add Inventory Availability (if requested)
    -- ========================================================================
    CREATE TABLE #InventoryStatus (
        PartNumber          VARCHAR(25),
        TotalOnHand         INT,
        WarehouseCount      INT,
        NearestWarehouse    VARCHAR(10)
    );

    IF @IncludeInventory = 1
    BEGIN
        INSERT INTO #InventoryStatus
        SELECT
            sc.PartNumber,
            ISNULL(SUM(pi.OnHandQty), 0),
            COUNT(DISTINCT pi.WarehouseCode),
            (SELECT TOP 1 WarehouseCode FROM dbo.PartInventory WITH (NOLOCK)
             WHERE PartNumber = sc.PartNumber AND OnHandQty > 0
             ORDER BY OnHandQty DESC)
        FROM #SupersessionChain sc
        LEFT JOIN dbo.PartInventory pi WITH (NOLOCK)
            ON pi.PartNumber = sc.PartNumber
        GROUP BY sc.PartNumber;
    END

    -- ========================================================================
    -- Step 5: Return Combined Results
    -- ========================================================================
    SELECT
        sc.ChainPosition,
        sc.PartNumber,
        sc.Description,
        sc.PartGroupCode,
        sc.UnitCost,
        sc.ListPrice,
        sc.Status,
        sc.SupplierCode,
        sc.IsOriginalRequest,
        sc.IsCurrentActive,
        fr.FitsVehicle,
        fr.FitmentNotes,
        inv.TotalOnHand,
        inv.WarehouseCount,
        inv.NearestWarehouse
    FROM #SupersessionChain sc
    LEFT JOIN #FitmentResults fr ON fr.PartNumber = sc.PartNumber
    LEFT JOIN #InventoryStatus inv ON inv.PartNumber = sc.PartNumber
    ORDER BY sc.ChainPosition;

    -- Cleanup
    DROP TABLE #SupersessionChain;
    DROP TABLE #FitmentResults;
    DROP TABLE #InventoryStatus;
END
GO

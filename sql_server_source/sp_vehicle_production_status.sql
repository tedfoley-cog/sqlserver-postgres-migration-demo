-- ============================================================================
-- sp_VehicleProductionStatus
-- SQL Server Stored Procedure — Vehicle Production Tracking Report
-- Plant Operations Module
--
-- Purpose: Generates a production status report for a plant and date range.
--          Tracks each vehicle through assembly stations, calculates throughput
--          metrics, identifies bottlenecks, and assigns VINs at final quality
--          gate. Produces a summary with station-level cycle time analysis.
--
-- Called by: Plant Manager Dashboard, Morning Stand-Up Report (automated),
--           Shift Handover Report (manual trigger)
--
-- NOTE: Station sequencing logic, bottleneck detection thresholds, and the
--       VIN assignment rules are embedded here and not documented elsewhere.
--
-- History:
--   2010-05-14  D. Martinez     Created (replaced whiteboard tracking)
--   2013-09-22  H. Yamamoto     Added station bottleneck detection
--   2014-12-01  C. Oduya        Added shift pattern handling
--   2017-06-30  R. Bergstrom    Added VIN assignment at quality gate
--   2021-02-14  P. Gupta        Optimized with CROSS APPLY for BOM status
-- ============================================================================

CREATE PROCEDURE dbo.sp_VehicleProductionStatus
    @PlantCode          VARCHAR(5),
    @StartDate          DATETIME,
    @EndDate            DATETIME,
    @StationFilter      VARCHAR(15) = NULL,  -- NULL = all stations
    @IncludeCompleted   BIT = 1
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    -- ========================================================================
    -- Step 1: Get vehicles in production for the date range
    -- ========================================================================
    CREATE TABLE #VehiclesInScope (
        VehicleID           INT,
        VIN                 VARCHAR(17),
        ModelCode           VARCHAR(10),
        ModelName           NVARCHAR(50),
        TrimLevel           VARCHAR(20),
        AssemblyDate        DATETIME,
        CurrentStationCode  VARCHAR(15),
        CurrentStationName  NVARCHAR(50),
        CurrentStatus       VARCHAR(20),
        StationsCompleted   INT,
        TotalStations       INT,
        ProgressPct         DECIMAL(5,2),
        TotalBuildMinutes   DECIMAL(10,2),
        IsOverCycleTime     BIT DEFAULT 0
    );

    INSERT INTO #VehiclesInScope (
        VehicleID, VIN, ModelCode, ModelName, TrimLevel, AssemblyDate,
        CurrentStationCode, CurrentStationName, CurrentStatus,
        StationsCompleted, TotalStations, ProgressPct, TotalBuildMinutes
    )
    SELECT
        v.VehicleID,
        v.VIN,
        v.ModelCode,
        v.ModelName,
        v.TrimLevel,
        v.AssemblyDate,
        ps.StationCode,
        ps.StationName,
        pt.Status,
        -- Stations completed = count of COMPLETED tracking records
        (SELECT COUNT(*) FROM dbo.ProductionTracking pt2 WITH (NOLOCK)
         WHERE pt2.VehicleID = v.VehicleID AND pt2.Status = 'COMPLETED'),
        -- Total stations for this plant
        (SELECT COUNT(*) FROM dbo.ProductionStation ps2 WITH (NOLOCK)
         WHERE ps2.PlantCode = @PlantCode),
        -- Progress %
        CASE
            WHEN (SELECT COUNT(*) FROM dbo.ProductionStation ps3 WITH (NOLOCK)
                  WHERE ps3.PlantCode = @PlantCode) = 0 THEN 0
            ELSE ROUND(
                CAST((SELECT COUNT(*) FROM dbo.ProductionTracking pt3 WITH (NOLOCK)
                      WHERE pt3.VehicleID = v.VehicleID AND pt3.Status = 'COMPLETED') AS DECIMAL)
                / CAST((SELECT COUNT(*) FROM dbo.ProductionStation ps4 WITH (NOLOCK)
                        WHERE ps4.PlantCode = @PlantCode) AS DECIMAL) * 100, 1)
        END,
        -- Total build minutes so far
        ISNULL((SELECT SUM(DATEDIFF(MINUTE, pt4.EntryTime, ISNULL(pt4.ExitTime, GETDATE())))
                FROM dbo.ProductionTracking pt4 WITH (NOLOCK)
                WHERE pt4.VehicleID = v.VehicleID), 0)
    FROM dbo.Vehicle v WITH (NOLOCK)
    LEFT JOIN dbo.ProductionTracking pt WITH (NOLOCK)
        ON pt.VehicleID = v.VehicleID
        AND pt.ExitTime IS NULL              -- currently at this station
    LEFT JOIN dbo.ProductionStation ps WITH (NOLOCK)
        ON ps.StationID = pt.StationID
    WHERE v.PlantCode = @PlantCode
      AND v.AssemblyDate >= @StartDate
      AND v.AssemblyDate < @EndDate
      AND (@IncludeCompleted = 1 OR v.Status != 'SHIPPED')
      AND (@StationFilter IS NULL OR ps.StationCode = @StationFilter);

    -- ========================================================================
    -- Step 2: Flag over-cycle-time vehicles (bottleneck detection)
    --         A vehicle is "over cycle" if its time at the current station
    --         exceeds 1.5x the standard cycle time for that station.
    -- ========================================================================
    UPDATE vis
    SET IsOverCycleTime = 1
    FROM #VehiclesInScope vis
    INNER JOIN dbo.ProductionStation ps WITH (NOLOCK)
        ON ps.StationCode = vis.CurrentStationCode
        AND ps.PlantCode = @PlantCode
    INNER JOIN dbo.ProductionTracking pt WITH (NOLOCK)
        ON pt.VehicleID = vis.VehicleID
        AND pt.StationID = ps.StationID
        AND pt.ExitTime IS NULL
    WHERE DATEDIFF(MINUTE, pt.EntryTime, GETDATE()) > (ps.CycleTimeMinutes * 1.5);

    -- ========================================================================
    -- Step 3: Station-Level Summary (throughput, avg cycle time, bottleneck count)
    -- ========================================================================
    CREATE TABLE #StationMetrics (
        StationCode         VARCHAR(15),
        StationName         NVARCHAR(50),
        SequenceOrder       INT,
        StandardCycleTime   DECIMAL(8,2),
        AvgActualCycleTime  DECIMAL(8,2),
        VehiclesCompleted   INT,
        VehiclesInStation   INT,
        VehiclesOverCycle   INT,
        CycleTimeVariance   DECIMAL(8,2),
        BottleneckFlag      VARCHAR(10)
    );

    INSERT INTO #StationMetrics
    SELECT
        ps.StationCode,
        ps.StationName,
        ps.SequenceOrder,
        ps.CycleTimeMinutes,
        -- Avg actual cycle time for completed vehicles at this station
        ISNULL((
            SELECT AVG(CAST(DATEDIFF(MINUTE, pt.EntryTime, pt.ExitTime) AS DECIMAL))
            FROM dbo.ProductionTracking pt WITH (NOLOCK)
            WHERE pt.StationID = ps.StationID
              AND pt.Status = 'COMPLETED'
              AND pt.EntryTime >= @StartDate
              AND pt.EntryTime < @EndDate
        ), 0),
        -- Vehicles completed at this station in range
        (SELECT COUNT(*)
         FROM dbo.ProductionTracking pt WITH (NOLOCK)
         WHERE pt.StationID = ps.StationID
           AND pt.Status = 'COMPLETED'
           AND pt.EntryTime >= @StartDate),
        -- Vehicles currently at this station
        (SELECT COUNT(*)
         FROM dbo.ProductionTracking pt WITH (NOLOCK)
         WHERE pt.StationID = ps.StationID
           AND pt.ExitTime IS NULL),
        -- Vehicles over cycle time at this station
        (SELECT COUNT(*)
         FROM dbo.ProductionTracking pt WITH (NOLOCK)
         WHERE pt.StationID = ps.StationID
           AND pt.ExitTime IS NULL
           AND DATEDIFF(MINUTE, pt.EntryTime, GETDATE()) > (ps.CycleTimeMinutes * 1.5)),
        -- Cycle time variance = (avg actual - standard) / standard * 100
        CASE
            WHEN ps.CycleTimeMinutes = 0 THEN 0
            ELSE ROUND((
                ISNULL((
                    SELECT AVG(CAST(DATEDIFF(MINUTE, pt.EntryTime, pt.ExitTime) AS DECIMAL))
                    FROM dbo.ProductionTracking pt WITH (NOLOCK)
                    WHERE pt.StationID = ps.StationID
                      AND pt.Status = 'COMPLETED'
                      AND pt.EntryTime >= @StartDate
                ), 0) - ps.CycleTimeMinutes
            ) / ps.CycleTimeMinutes * 100, 1)
        END,
        -- Bottleneck flag: RED if variance > 25%, YELLOW if > 10%
        CASE
            WHEN ps.CycleTimeMinutes > 0 AND (
                ISNULL((
                    SELECT AVG(CAST(DATEDIFF(MINUTE, pt.EntryTime, pt.ExitTime) AS DECIMAL))
                    FROM dbo.ProductionTracking pt WITH (NOLOCK)
                    WHERE pt.StationID = ps.StationID
                      AND pt.Status = 'COMPLETED'
                      AND pt.EntryTime >= @StartDate
                ), 0) - ps.CycleTimeMinutes
            ) / ps.CycleTimeMinutes > 0.25 THEN 'RED'
            WHEN ps.CycleTimeMinutes > 0 AND (
                ISNULL((
                    SELECT AVG(CAST(DATEDIFF(MINUTE, pt.EntryTime, pt.ExitTime) AS DECIMAL))
                    FROM dbo.ProductionTracking pt WITH (NOLOCK)
                    WHERE pt.StationID = ps.StationID
                      AND pt.Status = 'COMPLETED'
                      AND pt.EntryTime >= @StartDate
                ), 0) - ps.CycleTimeMinutes
            ) / ps.CycleTimeMinutes > 0.10 THEN 'YELLOW'
            ELSE 'GREEN'
        END
    FROM dbo.ProductionStation ps WITH (NOLOCK)
    WHERE ps.PlantCode = @PlantCode;

    -- ========================================================================
    -- Step 4: VIN Assignment Check
    --         Vehicles at the final quality gate that pass inspection get
    --         their VIN officially registered (status → ASSEMBLED)
    -- ========================================================================
    DECLARE @FinalGateStationID INT;

    SELECT TOP 1 @FinalGateStationID = StationID
    FROM dbo.ProductionStation WITH (NOLOCK)
    WHERE PlantCode = @PlantCode
      AND IsQualityGate = 1
    ORDER BY SequenceOrder DESC;

    IF @FinalGateStationID IS NOT NULL
    BEGIN
        UPDATE v
        SET v.Status = 'ASSEMBLED',
            v.ModifiedDate = GETDATE()
        FROM dbo.Vehicle v
        INNER JOIN dbo.ProductionTracking pt
            ON pt.VehicleID = v.VehicleID
        WHERE pt.StationID = @FinalGateStationID
          AND pt.Status = 'COMPLETED'
          AND pt.DefectsFound = 0
          AND v.Status = 'IN_PRODUCTION';
    END

    -- ========================================================================
    -- Step 5: Return Results
    -- ========================================================================

    -- Result set 1: Vehicle-level detail
    SELECT * FROM #VehiclesInScope ORDER BY ProgressPct DESC, VehicleID;

    -- Result set 2: Station-level metrics
    SELECT * FROM #StationMetrics ORDER BY SequenceOrder;

    -- Result set 3: Plant summary
    SELECT
        @PlantCode AS PlantCode,
        COUNT(*) AS TotalVehicles,
        SUM(CASE WHEN CurrentStatus = 'COMPLETED' OR ProgressPct = 100 THEN 1 ELSE 0 END)
            AS CompletedVehicles,
        SUM(CASE WHEN IsOverCycleTime = 1 THEN 1 ELSE 0 END) AS OverCycleVehicles,
        AVG(ProgressPct) AS AvgProgressPct,
        AVG(TotalBuildMinutes) AS AvgBuildMinutes,
        (SELECT COUNT(*) FROM #StationMetrics WHERE BottleneckFlag = 'RED')
            AS RedBottlenecks,
        (SELECT COUNT(*) FROM #StationMetrics WHERE BottleneckFlag = 'YELLOW')
            AS YellowBottlenecks
    FROM #VehiclesInScope;

    -- Cleanup
    DROP TABLE #VehiclesInScope;
    DROP TABLE #StationMetrics;
END
GO

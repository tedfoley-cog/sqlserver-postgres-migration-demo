-- ============================================================================
-- Vehicle Operations Database — SQL Server Schema
-- Auto Manufacturing — On-Premises SQL Server 2019
--
-- Core tables for vehicle production, warranty, parts, and supplier management.
-- Business logic is primarily in stored procedures (see sp_*.sql files).
--
-- History:
--   2008-04-10  Initial schema (migrated from Access)
--   2012-07-15  Added warranty tables
--   2015-11-20  Added supplier quality tracking
--   2019-03-08  Added parts supersession chain
--   2022-09-12  Added indexes for reporting performance
-- ============================================================================

-- ── Vehicle & Production ─────────────────────────────────────────────────────

CREATE TABLE dbo.Vehicle (
    VehicleID           INT IDENTITY(1,1) PRIMARY KEY,
    VIN                 VARCHAR(17) NOT NULL UNIQUE,
    ModelYear           INT NOT NULL,
    ModelCode           VARCHAR(10) NOT NULL,
    ModelName           NVARCHAR(50) NOT NULL,
    TrimLevel           VARCHAR(20),
    EngineCode          VARCHAR(15),
    TransmissionType    VARCHAR(10),       -- 'AUTO', 'MANUAL', 'CVT', 'DCT'
    PlantCode           VARCHAR(5) NOT NULL,
    AssemblyDate        DATETIME,
    ShipDate            DATETIME,
    DealerCode          VARCHAR(10),
    CurrentMileage      INT DEFAULT 0,
    WarrantyStartDate   DATETIME,
    WarrantyEndDate     DATETIME,
    Status              VARCHAR(20) DEFAULT 'IN_PRODUCTION',
    CreatedDate         DATETIME DEFAULT GETDATE(),
    ModifiedDate        DATETIME DEFAULT GETDATE()
);

CREATE TABLE dbo.ProductionStation (
    StationID           INT IDENTITY(1,1) PRIMARY KEY,
    StationCode         VARCHAR(15) NOT NULL,
    StationName         NVARCHAR(50) NOT NULL,
    PlantCode           VARCHAR(5) NOT NULL,
    SequenceOrder       INT NOT NULL,
    CycleTimeMinutes    DECIMAL(8,2),
    IsQualityGate       BIT DEFAULT 0
);

CREATE TABLE dbo.ProductionTracking (
    TrackingID          INT IDENTITY(1,1) PRIMARY KEY,
    VehicleID           INT NOT NULL REFERENCES dbo.Vehicle(VehicleID),
    StationID           INT NOT NULL REFERENCES dbo.ProductionStation(StationID),
    EntryTime           DATETIME NOT NULL,
    ExitTime            DATETIME,
    OperatorID          VARCHAR(20),
    Status              VARCHAR(20) DEFAULT 'IN_STATION',  -- IN_STATION, COMPLETED, REWORK, HOLD
    DefectsFound        INT DEFAULT 0,
    Notes               NVARCHAR(500)
);

-- ── Warranty ─────────────────────────────────────────────────────────────────

CREATE TABLE dbo.WarrantyClaim (
    ClaimID             INT IDENTITY(1,1) PRIMARY KEY,
    ClaimNumber         VARCHAR(20) NOT NULL UNIQUE,
    VehicleID           INT NOT NULL REFERENCES dbo.Vehicle(VehicleID),
    DealerCode          VARCHAR(10) NOT NULL,
    ClaimDate           DATETIME NOT NULL,
    MileageAtClaim      INT NOT NULL,
    SymptomCode         VARCHAR(10),
    CausalPartNumber    VARCHAR(25),
    LaborOperationCode  VARCHAR(15),
    LaborHours          DECIMAL(6,2),
    LaborRate           DECIMAL(8,2),
    PartsCost           DECIMAL(10,2),
    SubletCost          DECIMAL(10,2) DEFAULT 0,
    Deductible          DECIMAL(8,2) DEFAULT 0,
    TotalAmount         DECIMAL(10,2),
    CoverageType        VARCHAR(20),       -- 'BUMPER_TO_BUMPER', 'POWERTRAIN', 'EMISSIONS', 'CORROSION'
    ClaimStatus         VARCHAR(15) DEFAULT 'SUBMITTED',  -- SUBMITTED, APPROVED, DENIED, SETTLED
    DenialReasonCode    VARCHAR(10),
    ApprovedDate        DATETIME,
    SettledDate         DATETIME,
    SubmittedBy         VARCHAR(50),
    CreatedDate         DATETIME DEFAULT GETDATE()
);

CREATE TABLE dbo.WarrantyCoverage (
    CoverageID          INT IDENTITY(1,1) PRIMARY KEY,
    CoverageType        VARCHAR(20) NOT NULL,
    ModelYear           INT NOT NULL,
    MonthsFromSale      INT NOT NULL,
    MileageLimit        INT NOT NULL,
    Description         NVARCHAR(100),
    DeductibleAmount    DECIMAL(8,2) DEFAULT 0,
    PartGroupCodes      VARCHAR(200)       -- comma-separated part group codes covered
);

CREATE TABLE dbo.LaborRateSchedule (
    RateID              INT IDENTITY(1,1) PRIMARY KEY,
    RegionCode          VARCHAR(10) NOT NULL,
    DealerTier          VARCHAR(5) NOT NULL,   -- 'A', 'B', 'C'
    LaborType           VARCHAR(20) NOT NULL,  -- 'MECHANICAL', 'ELECTRICAL', 'BODY'
    HourlyRate          DECIMAL(8,2) NOT NULL,
    EffectiveDate       DATETIME NOT NULL,
    ExpirationDate      DATETIME
);

-- ── Parts ────────────────────────────────────────────────────────────────────

CREATE TABLE dbo.Part (
    PartID              INT IDENTITY(1,1) PRIMARY KEY,
    PartNumber          VARCHAR(25) NOT NULL UNIQUE,
    Description         NVARCHAR(100) NOT NULL,
    PartGroupCode       VARCHAR(10),
    UnitCost            DECIMAL(10,2),
    ListPrice           DECIMAL(10,2),
    Weight              DECIMAL(8,3),
    UnitOfMeasure       VARCHAR(5) DEFAULT 'EA',
    Status              VARCHAR(15) DEFAULT 'ACTIVE',   -- ACTIVE, SUPERSEDED, DISCONTINUED
    SupersededByPartNo  VARCHAR(25),
    SupplierCode        VARCHAR(20),
    LeadTimeDays        INT,
    MinOrderQty         INT DEFAULT 1,
    CreatedDate         DATETIME DEFAULT GETDATE()
);

CREATE TABLE dbo.PartFitment (
    FitmentID           INT IDENTITY(1,1) PRIMARY KEY,
    PartNumber          VARCHAR(25) NOT NULL,
    ModelCode           VARCHAR(10) NOT NULL,
    ModelYearFrom       INT NOT NULL,
    ModelYearTo         INT NOT NULL,
    TrimLevel           VARCHAR(20),          -- NULL = all trims
    EngineCode          VARCHAR(15),          -- NULL = all engines
    Notes               NVARCHAR(200)
);

CREATE TABLE dbo.PartInventory (
    InventoryID         INT IDENTITY(1,1) PRIMARY KEY,
    PartNumber          VARCHAR(25) NOT NULL,
    WarehouseCode       VARCHAR(10) NOT NULL,
    OnHandQty           INT DEFAULT 0,
    ReorderPoint        INT DEFAULT 0,
    LastCountDate       DATETIME
);

-- ── Suppliers ────────────────────────────────────────────────────────────────

CREATE TABLE dbo.Supplier (
    SupplierID          INT IDENTITY(1,1) PRIMARY KEY,
    SupplierCode        VARCHAR(20) NOT NULL UNIQUE,
    SupplierName        NVARCHAR(100) NOT NULL,
    ContactName         NVARCHAR(50),
    Region              VARCHAR(10),
    Country             VARCHAR(30),
    QualityRating       VARCHAR(5),        -- 'A+', 'A', 'B', 'C', 'PROB'
    IsApproved          BIT DEFAULT 1,
    CertificationExpiry DATETIME,
    AnnualVolume        INT,
    OnTimeDeliveryPct   DECIMAL(5,2),
    DefectPPM           INT,               -- defects per million
    CreatedDate         DATETIME DEFAULT GETDATE()
);

CREATE TABLE dbo.SupplierShipment (
    ShipmentID          INT IDENTITY(1,1) PRIMARY KEY,
    SupplierCode        VARCHAR(20) NOT NULL,
    PurchaseOrderNo     VARCHAR(20),
    PartNumber          VARCHAR(25) NOT NULL,
    ShipDate            DATETIME NOT NULL,
    ReceivedDate        DATETIME,
    OrderedQty          INT NOT NULL,
    ReceivedQty         INT,
    RejectedQty         INT DEFAULT 0,
    DueDate             DATETIME NOT NULL,
    IsOnTime            BIT,
    InspectionResult    VARCHAR(15)        -- 'PASS', 'FAIL', 'CONDITIONAL'
);

CREATE TABLE dbo.DealerSettlement (
    SettlementID        INT IDENTITY(1,1) PRIMARY KEY,
    SettlementBatchNo   VARCHAR(20) NOT NULL,
    DealerCode          VARCHAR(10) NOT NULL,
    ClaimID             INT REFERENCES dbo.WarrantyClaim(ClaimID),
    LaborAmount         DECIMAL(10,2),
    PartsAmount         DECIMAL(10,2),
    PartsMarkup         DECIMAL(10,2),
    SubletAmount        DECIMAL(10,2),
    DeductibleCredit    DECIMAL(8,2) DEFAULT 0,
    NetAmount           DECIMAL(10,2),
    SettlementDate      DATETIME,
    PaymentStatus       VARCHAR(15) DEFAULT 'PENDING'
);

-- ── Indexes ──────────────────────────────────────────────────────────────────

CREATE INDEX IX_Vehicle_VIN ON dbo.Vehicle(VIN);
CREATE INDEX IX_Vehicle_PlantCode ON dbo.Vehicle(PlantCode);
CREATE INDEX IX_Vehicle_ModelYear ON dbo.Vehicle(ModelYear, ModelCode);
CREATE INDEX IX_WarrantyClaim_VehicleID ON dbo.WarrantyClaim(VehicleID);
CREATE INDEX IX_WarrantyClaim_Status ON dbo.WarrantyClaim(ClaimStatus);
CREATE INDEX IX_WarrantyClaim_Date ON dbo.WarrantyClaim(ClaimDate);
CREATE INDEX IX_Part_PartNumber ON dbo.Part(PartNumber);
CREATE INDEX IX_Part_Supersession ON dbo.Part(SupersededByPartNo);
CREATE INDEX IX_PartFitment_Model ON dbo.PartFitment(ModelCode, ModelYearFrom, ModelYearTo);
CREATE INDEX IX_Supplier_Code ON dbo.Supplier(SupplierCode);
CREATE INDEX IX_SupplierShipment_Supplier ON dbo.SupplierShipment(SupplierCode);
CREATE INDEX IX_ProductionTracking_Vehicle ON dbo.ProductionTracking(VehicleID);

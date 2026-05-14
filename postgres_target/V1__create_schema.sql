-- ============================================================================
-- Vehicle Operations — PostgreSQL 16 Schema
-- Migrated from SQL Server 2019
--
-- All business logic has been extracted into the Spring Boot application layer.
-- This schema contains only table definitions, constraints, and indexes —
-- no stored procedures or functions.
-- ============================================================================

-- ── Vehicle & Production ─────────────────────────────────────────────────────

CREATE TABLE vehicle (
    vehicle_id          SERIAL PRIMARY KEY,
    vin                 VARCHAR(17) NOT NULL UNIQUE,
    model_year          INT NOT NULL,
    model_code          VARCHAR(10) NOT NULL,
    model_name          VARCHAR(50) NOT NULL,
    trim_level          VARCHAR(20),
    engine_code         VARCHAR(15),
    transmission_type   VARCHAR(10),
    plant_code          VARCHAR(5) NOT NULL,
    assembly_date       TIMESTAMP,
    ship_date           TIMESTAMP,
    dealer_code         VARCHAR(10),
    current_mileage     INT DEFAULT 0,
    warranty_start_date TIMESTAMP,
    warranty_end_date   TIMESTAMP,
    status              VARCHAR(20) DEFAULT 'IN_PRODUCTION',
    created_date        TIMESTAMP DEFAULT NOW(),
    modified_date       TIMESTAMP DEFAULT NOW()
);

CREATE TABLE production_station (
    station_id          SERIAL PRIMARY KEY,
    station_code        VARCHAR(15) NOT NULL,
    station_name        VARCHAR(50) NOT NULL,
    plant_code          VARCHAR(5) NOT NULL,
    sequence_order      INT NOT NULL,
    cycle_time_minutes  NUMERIC(8,2),
    is_quality_gate     BOOLEAN DEFAULT FALSE
);

CREATE TABLE production_tracking (
    tracking_id         SERIAL PRIMARY KEY,
    vehicle_id          INT NOT NULL REFERENCES vehicle(vehicle_id),
    station_id          INT NOT NULL REFERENCES production_station(station_id),
    entry_time          TIMESTAMP NOT NULL,
    exit_time           TIMESTAMP,
    operator_id         VARCHAR(20),
    status              VARCHAR(20) DEFAULT 'IN_STATION',
    defects_found       INT DEFAULT 0,
    notes               VARCHAR(500)
);

-- ── Warranty ─────────────────────────────────────────────────────────────────

CREATE SEQUENCE claim_number_seq START WITH 100;

CREATE TABLE warranty_claim (
    claim_id            SERIAL PRIMARY KEY,
    claim_number        VARCHAR(20) NOT NULL UNIQUE,
    vehicle_id          INT NOT NULL REFERENCES vehicle(vehicle_id),
    dealer_code         VARCHAR(10) NOT NULL,
    claim_date          TIMESTAMP NOT NULL,
    mileage_at_claim    INT NOT NULL,
    symptom_code        VARCHAR(10),
    causal_part_number  VARCHAR(25),
    labor_operation_code VARCHAR(15),
    labor_hours         NUMERIC(6,2),
    labor_rate          NUMERIC(8,2),
    parts_cost          NUMERIC(10,2),
    sublet_cost         NUMERIC(10,2) DEFAULT 0,
    deductible          NUMERIC(8,2) DEFAULT 0,
    total_amount        NUMERIC(10,2),
    coverage_type       VARCHAR(20),
    claim_status        VARCHAR(15) DEFAULT 'SUBMITTED',
    denial_reason_code  VARCHAR(10),
    approved_date       TIMESTAMP,
    settled_date        TIMESTAMP,
    submitted_by        VARCHAR(50),
    version             INT DEFAULT 0 NOT NULL,
    created_date        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE warranty_coverage (
    coverage_id         SERIAL PRIMARY KEY,
    coverage_type       VARCHAR(20) NOT NULL,
    model_year          INT NOT NULL,
    months_from_sale    INT NOT NULL,
    mileage_limit       INT NOT NULL,
    description         VARCHAR(100),
    deductible_amount   NUMERIC(8,2) DEFAULT 0,
    part_group_codes    VARCHAR(200)
);

CREATE TABLE labor_rate_schedule (
    rate_id             SERIAL PRIMARY KEY,
    region_code         VARCHAR(10) NOT NULL,
    dealer_tier         VARCHAR(5) NOT NULL,
    labor_type          VARCHAR(20) NOT NULL,
    hourly_rate         NUMERIC(8,2) NOT NULL,
    effective_date      TIMESTAMP NOT NULL,
    expiration_date     TIMESTAMP
);

-- ── Parts ────────────────────────────────────────────────────────────────────

CREATE TABLE part (
    part_id             SERIAL PRIMARY KEY,
    part_number         VARCHAR(25) NOT NULL UNIQUE,
    description         VARCHAR(100) NOT NULL,
    part_group_code     VARCHAR(10),
    unit_cost           NUMERIC(10,2),
    list_price          NUMERIC(10,2),
    weight              NUMERIC(8,3),
    unit_of_measure     VARCHAR(5) DEFAULT 'EA',
    status              VARCHAR(15) DEFAULT 'ACTIVE',
    superseded_by_part_no VARCHAR(25),
    supplier_code       VARCHAR(20),
    lead_time_days      INT,
    min_order_qty       INT DEFAULT 1,
    created_date        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE part_fitment (
    fitment_id          SERIAL PRIMARY KEY,
    part_number         VARCHAR(25) NOT NULL,
    model_code          VARCHAR(10) NOT NULL,
    model_year_from     INT NOT NULL,
    model_year_to       INT NOT NULL,
    trim_level          VARCHAR(20),
    engine_code         VARCHAR(15),
    notes               VARCHAR(200)
);

CREATE TABLE part_inventory (
    inventory_id        SERIAL PRIMARY KEY,
    part_number         VARCHAR(25) NOT NULL,
    warehouse_code      VARCHAR(10) NOT NULL,
    on_hand_qty         INT DEFAULT 0,
    reorder_point       INT DEFAULT 0,
    last_count_date     TIMESTAMP
);

-- ── Suppliers ────────────────────────────────────────────────────────────────

CREATE TABLE supplier (
    supplier_id         SERIAL PRIMARY KEY,
    supplier_code       VARCHAR(20) NOT NULL UNIQUE,
    supplier_name       VARCHAR(100) NOT NULL,
    contact_name        VARCHAR(50),
    region              VARCHAR(10),
    country             VARCHAR(30),
    quality_rating      VARCHAR(5),
    is_approved         BOOLEAN DEFAULT TRUE,
    certification_expiry TIMESTAMP,
    annual_volume       INT,
    on_time_delivery_pct NUMERIC(5,2),
    defect_ppm          INT,
    created_date        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE supplier_shipment (
    shipment_id         SERIAL PRIMARY KEY,
    supplier_code       VARCHAR(20) NOT NULL,
    purchase_order_no   VARCHAR(20),
    part_number         VARCHAR(25) NOT NULL,
    ship_date           TIMESTAMP NOT NULL,
    received_date       TIMESTAMP,
    ordered_qty         INT NOT NULL,
    received_qty        INT,
    rejected_qty        INT DEFAULT 0,
    due_date            TIMESTAMP NOT NULL,
    is_on_time          BOOLEAN,
    inspection_result   VARCHAR(15)
);

CREATE TABLE dealer_settlement (
    settlement_id       SERIAL PRIMARY KEY,
    settlement_batch_no VARCHAR(20) NOT NULL,
    dealer_code         VARCHAR(10) NOT NULL,
    claim_id            INT REFERENCES warranty_claim(claim_id),
    labor_amount        NUMERIC(10,2),
    parts_amount        NUMERIC(10,2),
    parts_markup        NUMERIC(10,2),
    sublet_amount       NUMERIC(10,2),
    deductible_credit   NUMERIC(8,2) DEFAULT 0,
    net_amount          NUMERIC(10,2),
    settlement_date     TIMESTAMP,
    payment_status      VARCHAR(15) DEFAULT 'PENDING'
);

-- ── Indexes ──────────────────────────────────────────────────────────────────

CREATE INDEX idx_vehicle_vin ON vehicle(vin);
CREATE INDEX idx_vehicle_plant_code ON vehicle(plant_code);
CREATE INDEX idx_vehicle_model_year ON vehicle(model_year, model_code);
CREATE INDEX idx_warranty_claim_vehicle_id ON warranty_claim(vehicle_id);
CREATE INDEX idx_warranty_claim_status ON warranty_claim(claim_status);
CREATE INDEX idx_warranty_claim_date ON warranty_claim(claim_date);
CREATE INDEX idx_part_part_number ON part(part_number);
CREATE INDEX idx_part_supersession ON part(superseded_by_part_no);
CREATE INDEX idx_part_fitment_model ON part_fitment(model_code, model_year_from, model_year_to);
CREATE INDEX idx_supplier_code ON supplier(supplier_code);
CREATE INDEX idx_supplier_shipment_supplier ON supplier_shipment(supplier_code);
CREATE INDEX idx_production_tracking_vehicle ON production_tracking(vehicle_id);

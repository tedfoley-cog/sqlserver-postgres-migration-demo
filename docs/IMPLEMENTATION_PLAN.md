# Implementation Plan — SQL Server to PostgreSQL Migration Demo

## What the Demo Proves

A large auto manufacturer has accumulated years of business logic inside SQL Server stored procedures — warranty claim processing, VIN decoding, parts supersession lookups, supplier quality scoring. These stored procedures are undocumented, untested, and tightly coupled to SQL Server. This demo proves that Devin can **analyze undocumented stored procedures, extract the business logic into Spring Boot services, migrate the database to PostgreSQL, and generate tests** — all in a single live session.

## What Devin Does Live

Devin reads each stored procedure, reverse-engineers the business logic, extracts it into properly structured Spring Boot service classes with JPA repositories, generates PostgreSQL-compatible DDL (simple CRUD tables — no stored procedures), writes unit tests for all extracted business logic, and opens a PR with the complete migration.

## Stack and Rationale

| Component | Choice | Source |
|---|---|---|
| Legacy DB | SQL Server (T-SQL stored procedures) | [Microsoft T-SQL reference](https://learn.microsoft.com/en-us/sql/t-sql/language-reference) |
| Target DB | PostgreSQL 15 | Customer target (GCP Cloud SQL) |
| App framework | Spring Boot 2.7.18 (legacy) → Spring Boot 3.x (target) | [Spring Boot docs](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/) |
| SP calling pattern | `JdbcTemplate.call()` with `SimpleJdbcCall` | [Spring JdbcTemplate docs](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html) |
| Dev-runnable DB | H2 in-memory with SQL Server compatibility mode | [H2 SQL Server mode](https://h2database.com/html/features.html#compatibility) |
| Dashboard | Thymeleaf + vanilla CSS | Bundled with Spring Boot |
| VIN structure | 17-char format per SAE J853 / ISO 3779 | [NHTSA VIN standard](https://www.nhtsa.gov/vin-decoder) |
| Build tool | Maven | Industry standard for enterprise Java |

## Repo Layout

```
sqlserver-postgres-migration-demo/
├── docs/
│   ├── IMPLEMENTATION_PLAN.md        ← This file
│   ├── flowchart.html                ← Interactive demo flow diagram
│   └── flowchart.png                 ← Rasterized fallback
│
├── sql_server_source/                ← Original T-SQL (the legacy code Devin analyzes)
│   ├── tables/
│   │   └── schema.sql                ← Table definitions (DDL)
│   ├── sp_process_warranty_claim.sql ← Warranty claim validation + processing (~200 lines)
│   ├── sp_vehicle_production_status.sql ← Production tracking + VIN assignment (~180 lines)
│   ├── sp_parts_supersession.sql     ← Parts chain lookup + fitment check (~150 lines)
│   ├── sp_supplier_quality_scorecard.sql ← Supplier metrics aggregation (~170 lines)
│   ├── sp_dealer_settlement.sql      ← Dealer warranty reimbursement calc (~160 lines)
│   └── fn_decode_vin.sql             ← VIN decoder scalar function (~120 lines)
│
├── spring-boot-app/                  ← Legacy Spring Boot app (calls SPs via JdbcTemplate)
│   ├── pom.xml
│   ├── src/main/java/com/acme/vehicleops/
│   │   ├── VehicleOpsApplication.java
│   │   ├── config/
│   │   │   └── DatabaseConfig.java
│   │   ├── controller/
│   │   │   ├── WarrantyController.java
│   │   │   ├── ProductionController.java
│   │   │   ├── PartsController.java
│   │   │   └── DashboardController.java
│   │   ├── model/
│   │   │   ├── WarrantyClaim.java
│   │   │   ├── Vehicle.java
│   │   │   ├── Part.java
│   │   │   └── Supplier.java
│   │   ├── repository/               ← Thin wrappers — just call SPs via JdbcTemplate
│   │   │   ├── WarrantyRepository.java
│   │   │   ├── ProductionRepository.java
│   │   │   └── PartsRepository.java
│   │   └── service/                  ← Mostly empty — logic lives in the SPs
│   │       ├── WarrantyService.java
│   │       └── ProductionService.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── schema.sql                ← H2-compatible schema
│   │   ├── data.sql                  ← Seed data
│   │   └── templates/
│   │       └── dashboard.html        ← Simple Thymeleaf dashboard
│   └── src/test/java/                ← Empty — no tests exist (this is the pain point)
│       └── com/acme/vehicleops/
│
├── postgres_target/                  ← Empty — Devin writes migrated DDL here
│   └── .gitkeep
│
├── .github/workflows/
│   └── ci.yml                        ← Build + lint check
│
├── README.md
└── DEMO_NOTES.md
```

## Flowchart Outline

1. **Presenter Opens Repo** → shows stored procedures, points out no tests / no docs
2. **Prompt Devin** → "Migrate this from SQL Server to Postgres. Extract stored procedure logic into Spring Boot services. Write tests."
3. **Devin Analyzes SPs** → reads each SP, maps business rules
4. **Devin Extracts Logic** → creates Spring Boot services with extracted business rules
5. **Devin Writes JPA Entities** → replaces JdbcTemplate SP calls with JPA repositories
6. **Devin Generates Postgres DDL** → simple tables, no stored procedures
7. **Devin Writes Tests** → unit tests for all extracted business logic
8. **Devin Opens PR** → complete migration diff
9. **Presenter Reviews PR** → shows the diff, runs the app, shows dashboard

## Runtime Plan

The Spring Boot app runs against H2 in SQL Server compatibility mode (`MODE=MSSQLServer`). The app loads seed data automatically on startup. The dashboard at `http://localhost:8080/` shows vehicle, warranty, and parts data. During the live demo, Devin migrates the app to use JPA + Postgres-compatible schema and the dashboard continues to work against the new architecture.

**Commands:**
- `cd spring-boot-app && mvn spring-boot:run` → starts the app on port 8080
- `http://localhost:8080/` → dashboard with vehicle/warranty overview
- `http://localhost:8080/swagger-ui.html` → API documentation

## CI Plan

Simple GitHub Actions workflow:
- Checkout → setup JDK 8 → Maven build (`mvn -B verify`)
- No deployment, no matrix builds

## Risks and Unknowns

1. H2's SQL Server compatibility mode doesn't support all T-SQL syntax (e.g., `CROSS APPLY`, some date functions) — the schema.sql for H2 uses simplified equivalents.
2. The actual SQL Server → Postgres migration would need connection string changes and possibly Cloud SQL Proxy for GCP — not scaffolded here since it's infrastructure.
3. The VIN decoder uses hardcoded lookup tables per SAE J853 — in production these would be maintained as reference data.

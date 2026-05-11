# SQL Server to PostgreSQL Migration Demo

![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Java](https://img.shields.io/badge/Java-8-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-2.7.18-green)
![SQL Server](https://img.shields.io/badge/SQL_Server-T--SQL-red)

## Demo Workflow

> Extract stored procedure business logic into Spring Boot services, migrate to PostgreSQL, generate tests.
> For the full interactive version, see [`docs/flowchart.html`](docs/flowchart.html).

```mermaid
flowchart TD
  REPO[Demo Repository]
  PROMPT[Prompt Devin]

  subgraph ANALYSIS [Analyze Stored Procedures]
    direction TB
    A1[Read T-SQL Source]
    A2[Map Business Rules]
    A3[Identify Dependencies]
    A1 --> A2 --> A3
  end

  subgraph EXTRACT [Extract to Spring Boot]
    direction TB
    E1[Create Service Classes]
    E2[Write JPA Entities]
    E3[Build Repositories]
    E4[Update Controllers]
    E1 --> E2 --> E3 --> E4
  end

  subgraph MIGRATE [Migrate Database]
    direction TB
    M1[Generate Postgres DDL]
    M2[Convert Data Types]
    M3[Update Connection Config]
    M1 --> M2 --> M3
  end

  subgraph TEST [Generate Tests]
    direction TB
    T1[Unit Tests for Services]
    T2[Integration Tests]
    T3[Verify Equivalence]
    T1 --> T2 --> T3
  end

  BUILD[Build and Verify]
  DASHBOARD[Open Dashboard]
  PR[Open Pull Request]
  REVIEW[Presenter Reviews PR]

  REPO --> PROMPT
  PROMPT --> A1
  A3 --> E1
  A3 --> M1
  E4 --> BUILD
  M3 --> BUILD
  T3 --> BUILD
  BUILD --> DASHBOARD
  BUILD --> PR
  PR --> REVIEW

  classDef trigger    fill:#d1fae5,stroke:#059669,stroke-width:2px,color:#064e3b
  classDef analysis   fill:#e0e7ff,stroke:#6366f1,stroke-width:2px,color:#312e81
  classDef extract    fill:#dbeafe,stroke:#3b82f6,stroke-width:2px,color:#1e3a8a
  classDef migrate    fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#78350f
  classDef testing    fill:#f0fdf4,stroke:#22c55e,stroke-width:2px,color:#166534
  classDef output     fill:#fce7f3,stroke:#ec4899,stroke-width:2px,color:#831843
  classDef review     fill:#f3e8ff,stroke:#a855f7,stroke-width:2px,color:#581c87

  class REPO,PROMPT trigger
  class A1,A2,A3 analysis
  class E1,E2,E3,E4 extract
  class M1,M2,M3 migrate
  class T1,T2,T3 testing
  class BUILD,DASHBOARD,PR output
  class REVIEW review
```

<details><summary>Flowchart (PNG fallback)</summary>

![Demo Workflow](docs/flowchart.png)

</details>

---

## What This Demo Shows

A large auto manufacturer has accumulated **15+ years of business logic** inside SQL Server stored procedures — warranty claim processing, VIN decoding, parts supersession lookups, supplier quality scoring, dealer settlement calculations. These stored procedures are undocumented, untested, and tightly coupled to SQL Server. The company wants to migrate to PostgreSQL on GCP and move all stored procedure logic into the Spring Boot application layer.

This demo proves that Devin can analyze undocumented stored procedures, extract the business logic into properly structured Spring Boot services, migrate the database schema to PostgreSQL, and generate comprehensive tests — all in a single live session.

## What Devin Does Live

Devin reads each of the 6 stored procedures in `sql_server_source/`, reverse-engineers the business rules (warranty coverage priority, dealer region mapping, parts supersession chain walking, supplier scoring weights, settlement markup rules, VIN check-digit validation), extracts them into Spring Boot service classes with JPA repositories, generates PostgreSQL-compatible DDL in `postgres_target/` (simple CRUD tables — no stored procedures), writes unit and integration tests, updates the app configuration, and opens a PR with the complete migration. The dashboard at `localhost:8080` continues to work against the new architecture.

## How the Demo Runs

1. Presenter opens the repo and walks through the stored procedures, highlighting the undocumented business logic
2. Presenter prompts Devin in a fresh session with the migration instructions
3. Devin analyzes all 6 stored procedures, extracts the business rules, creates Spring Boot services, generates Postgres DDL and tests
4. Devin runs `mvn verify` to confirm everything builds and tests pass
5. Devin opens a PR with the complete migration diff
6. Presenter reviews the PR, runs the app, and shows the dashboard still works

### Local Development

```bash
cd spring-boot-app
mvn spring-boot:run
# Dashboard:   http://localhost:8080/
# Swagger UI:  http://localhost:8080/swagger-ui.html
# H2 Console:  http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:vehicleops)
```

---

## Repo Layout

```
sqlserver-postgres-migration-demo/
├── sql_server_source/                    ← Original T-SQL stored procedures (6 files)
│   ├── tables/schema.sql                 ← Table definitions
│   ├── sp_process_warranty_claim.sql     ← Warranty claim validation + processing
│   ├── sp_vehicle_production_status.sql  ← Production tracking + bottleneck detection
│   ├── sp_parts_supersession.sql         ← Parts chain lookup + fitment validation
│   ├── sp_supplier_quality_scorecard.sql ← Supplier scoring + trend analysis
│   ├── sp_dealer_settlement.sql          ← Dealer reimbursement calculation
│   └── fn_decode_vin.sql                 ← VIN decoder (SAE J853 / ISO 3779)
│
├── spring-boot-app/                      ← Legacy Spring Boot app (calls SPs via JdbcTemplate)
│   ├── pom.xml                           ← Spring Boot 2.7.18, Java 8
│   ├── src/main/java/.../controller/     ← REST + dashboard controllers
│   ├── src/main/java/.../repository/     ← Thin wrappers that call stored procedures
│   ├── src/main/java/.../service/        ← Mostly empty — logic lives in the SPs
│   ├── src/main/java/.../model/          ← JPA entities
│   └── src/main/resources/               ← H2 schema, seed data, Thymeleaf dashboard
│
├── postgres_target/                      ← Empty — Devin writes migrated DDL here
├── docs/                                 ← Flowchart + implementation plan
└── .github/workflows/ci.yml             ← Build + test
```

---

## Key Concepts

| Term | Description |
|---|---|
| **Stored Procedure (SP)** | T-SQL code block running inside SQL Server with embedded business logic |
| **VIN** | Vehicle Identification Number — 17 chars per SAE J853 / ISO 3779 |
| **Supersession Chain** | Part A → Part B → Part C replacement lineage |
| **Coverage Type** | Warranty tier: bumper-to-bumper, powertrain, emissions, corrosion |
| **PPM** | Parts Per Million — supplier defect rate metric |
| **Dealer Settlement** | Reimbursement batch processing for warranty work |
| **Parts Markup** | Dealer cost-plus calculation: OEM 40%, reman 25%, aftermarket 15% |
| **Check Digit** | VIN position 9 validation (mod-11 algorithm) |
| **NOLOCK** | SQL Server hint for dirty reads (no PostgreSQL equivalent needed) |
| **JdbcTemplate** | Spring's low-level JDBC abstraction used to call SPs |

# Demo Cheat Sheet — SQL Server to PostgreSQL Migration

## Setup (do this before joining the call)
- [ ] Open the repo in a browser tab: https://github.com/tedfoley-cog/sqlserver-postgres-migration-demo
- [ ] Have a fresh Devin session ready (or prepare to start one live)

## Demo Flow
1. Walk through `sql_server_source/` — point out the undocumented business logic in `sp_process_warranty_claim.sql` (coverage priority, dealer region mapping, high-value threshold) and `fn_decode_vin.sql` (check-digit algorithm, year-code table) — "15 years of logic with zero tests"
2. Prompt Devin: "Migrate this app from SQL Server to PostgreSQL. Extract all stored procedure business logic into Spring Boot service classes. Replace JdbcTemplate SP calls with JPA repositories. Generate unit tests for the extracted logic. Write the Postgres DDL in postgres_target/. Open a PR with everything."
3. While Devin works, narrate: it's reading each SP, mapping the business rules, creating properly structured services, writing tests that didn't exist before, generating clean Postgres DDL with no stored procedures
4. When Devin opens the PR, walk through the diff — show the warranty coverage logic now living in `WarrantyService.java` instead of T-SQL, the VIN decoder as a pure Java utility, the new test suite, the Postgres DDL
5. Run the app and show the dashboard still works: "Same functionality, but now the logic is in the application layer, fully tested, and running on Postgres"
6. "This is months of manual reverse-engineering and migration work — Devin did it in one session with full test coverage"

package com.acme.vehicleops.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.WarrantyClaim;

/**
 * Repository for warranty operations.
 *
 * All business logic is in the stored procedure sp_ProcessWarrantyClaim.
 * This class is a thin wrapper that marshals parameters and calls the SP.
 *
 * WARNING: The SP contains undocumented business rules for:
 *   - Coverage type determination (priority order: emissions, powertrain, corrosion, B2B)
 *   - Dealer region/tier mapping from dealer code prefix/suffix
 *   - Labor rate lookup by region, tier, and part group
 *   - High-value claim threshold ($2500)
 *   - Deductible application rules
 */
@Repository
public class WarrantyRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> processWarrantyClaim(
            String vin,
            String dealerCode,
            int mileageAtClaim,
            String symptomCode,
            String causalPartNumber,
            String laborOperationCode,
            BigDecimal laborHours,
            BigDecimal subletCost,
            String submittedBy) {

        // In production, this calls sp_ProcessWarrantyClaim on SQL Server.
        // For the demo (H2), we simulate the SP call with direct SQL.
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> vehicleData = jdbcTemplate.queryForMap(
                "SELECT VEHICLEID, MODELYEAR, MODELCODE, WARRANTYSTARTDATE " +
                "FROM VEHICLE WHERE VIN = ?", vin);

            if (vehicleData.isEmpty()) {
                result.put("claimStatus", "DENIED");
                result.put("denialReason", "DENY_01: Vehicle not found for VIN " + vin);
                return result;
            }

            // Simplified claim creation for H2 demo
            int nextId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(CLAIMID), 0) + 1 FROM WARRANTYCLAIM", Integer.class);
            String claimNumber = "WC-DEMO-" + String.format("%04d", nextId);

            jdbcTemplate.update(
                "INSERT INTO WARRANTYCLAIM (CLAIMNUMBER, VEHICLEID, DEALERCODE, CLAIMDATE, " +
                "MILEAGEATCLAIM, SYMPTOMCODE, CAUSALPARTNUMBER, LABOROPERATIONCODE, " +
                "LABORHOURS, PARTSCOST, SUBLETCOST, TOTALAMOUNT, CLAIMSTATUS, SUBMITTEDBY) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, " +
                "(SELECT UNITCOST FROM PART WHERE PARTNUMBER = ?), ?, ?, 'APPROVED', ?)",
                claimNumber, vehicleData.get("VEHICLEID"), dealerCode,
                mileageAtClaim, symptomCode, causalPartNumber, laborOperationCode,
                laborHours, causalPartNumber, subletCost,
                laborHours.multiply(new BigDecimal("85.00")),
                submittedBy);

            result.put("claimNumber", claimNumber);
            result.put("claimStatus", "APPROVED");
            result.put("totalAmount", laborHours.multiply(new BigDecimal("85.00")));
        } catch (Exception e) {
            result.put("claimStatus", "ERROR");
            result.put("denialReason", e.getMessage());
        }
        return result;
    }

    public List<WarrantyClaim> getRecentClaims(int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM WARRANTYCLAIM ORDER BY CLAIMDATE DESC LIMIT ?",
            new Object[]{limit},
            (rs, rowNum) -> {
                WarrantyClaim claim = new WarrantyClaim();
                claim.setClaimId(rs.getInt("CLAIMID"));
                claim.setClaimNumber(rs.getString("CLAIMNUMBER"));
                claim.setVehicleId(rs.getInt("VEHICLEID"));
                claim.setDealerCode(rs.getString("DEALERCODE"));
                claim.setClaimDate(rs.getTimestamp("CLAIMDATE"));
                claim.setMileageAtClaim(rs.getInt("MILEAGEATCLAIM"));
                claim.setCausalPartNumber(rs.getString("CAUSALPARTNUMBER"));
                claim.setTotalAmount(rs.getBigDecimal("TOTALAMOUNT"));
                claim.setClaimStatus(rs.getString("CLAIMSTATUS"));
                return claim;
            });
    }

    public Map<String, Object> getClaimSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalClaims", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM WARRANTYCLAIM", Integer.class));
        summary.put("approvedClaims", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM WARRANTYCLAIM WHERE CLAIMSTATUS = 'APPROVED'", Integer.class));
        summary.put("deniedClaims", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM WARRANTYCLAIM WHERE CLAIMSTATUS = 'DENIED'", Integer.class));
        summary.put("totalAmount", jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(TOTALAMOUNT), 0) FROM WARRANTYCLAIM WHERE CLAIMSTATUS = 'APPROVED'",
            BigDecimal.class));
        return summary;
    }
}

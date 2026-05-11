package com.acme.vehicleops.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.Part;

/**
 * Repository for parts catalog operations.
 *
 * In production, parts lookups go through sp_PartsSupersession which walks
 * the supersession chain, validates fitment, and checks inventory.
 * The chain-walking logic, fitment rules, and cross-reference mappings
 * are all embedded in the stored procedure.
 */
@Repository
public class PartsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Part> getAllParts() {
        return jdbcTemplate.query(
            "SELECT * FROM PART ORDER BY PARTNUMBER",
            (rs, rowNum) -> {
                Part p = new Part();
                p.setPartId(rs.getInt("PARTID"));
                p.setPartNumber(rs.getString("PARTNUMBER"));
                p.setDescription(rs.getString("DESCRIPTION"));
                p.setPartGroupCode(rs.getString("PARTGROUPCODE"));
                p.setUnitCost(rs.getBigDecimal("UNITCOST"));
                p.setListPrice(rs.getBigDecimal("LISTPRICE"));
                p.setStatus(rs.getString("STATUS"));
                p.setSupersededByPartNo(rs.getString("SUPERSEDEDBYPARTNO"));
                p.setSupplierCode(rs.getString("SUPPLIERCODE"));
                return p;
            });
    }

    public List<Map<String, Object>> getSupersessionChain(String partNumber) {
        // In production, this calls sp_PartsSupersession.
        // For H2 demo, we do a simplified iterative lookup.
        return jdbcTemplate.queryForList(
            "SELECT P1.PARTNUMBER AS ORIGINAL_PART, P1.DESCRIPTION, P1.STATUS, " +
            "P1.SUPERSEDEDBYPARTNO AS REPLACED_BY, " +
            "P2.DESCRIPTION AS REPLACEMENT_DESC, P2.STATUS AS REPLACEMENT_STATUS " +
            "FROM PART P1 " +
            "LEFT JOIN PART P2 ON P1.SUPERSEDEDBYPARTNO = P2.PARTNUMBER " +
            "WHERE P1.PARTNUMBER = ? OR P1.SUPERSEDEDBYPARTNO = ? " +
            "ORDER BY P1.PARTID",
            partNumber, partNumber);
    }

    public Map<String, Object> getPartsSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalParts", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM PART", Integer.class));
        summary.put("activeParts", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM PART WHERE STATUS = 'ACTIVE'", Integer.class));
        summary.put("supersededParts", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM PART WHERE STATUS = 'SUPERSEDED'", Integer.class));
        return summary;
    }
}

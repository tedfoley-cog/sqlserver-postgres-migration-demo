package com.acme.vehicleops.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.Vehicle;

/**
 * Repository for production tracking operations.
 *
 * In production, most queries go through sp_VehicleProductionStatus which
 * calculates throughput metrics, identifies bottlenecks, and assigns VINs.
 * For the H2 demo, we use direct queries as a simplified stand-in.
 */
@Repository
public class ProductionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Vehicle> getVehiclesByPlant(String plantCode) {
        return jdbcTemplate.query(
            "SELECT * FROM VEHICLE WHERE PLANTCODE = ? ORDER BY ASSEMBLYDATE DESC",
            new Object[]{plantCode},
            (rs, rowNum) -> {
                Vehicle v = new Vehicle();
                v.setVehicleId(rs.getInt("VEHICLEID"));
                v.setVin(rs.getString("VIN"));
                v.setModelYear(rs.getInt("MODELYEAR"));
                v.setModelCode(rs.getString("MODELCODE"));
                v.setModelName(rs.getString("MODELNAME"));
                v.setTrimLevel(rs.getString("TRIMLEVEL"));
                v.setPlantCode(rs.getString("PLANTCODE"));
                v.setAssemblyDate(rs.getTimestamp("ASSEMBLYDATE"));
                v.setStatus(rs.getString("STATUS"));
                return v;
            });
    }

    public List<Vehicle> getAllVehicles() {
        return jdbcTemplate.query(
            "SELECT * FROM VEHICLE ORDER BY VEHICLEID",
            (rs, rowNum) -> {
                Vehicle v = new Vehicle();
                v.setVehicleId(rs.getInt("VEHICLEID"));
                v.setVin(rs.getString("VIN"));
                v.setModelYear(rs.getInt("MODELYEAR"));
                v.setModelCode(rs.getString("MODELCODE"));
                v.setModelName(rs.getString("MODELNAME"));
                v.setTrimLevel(rs.getString("TRIMLEVEL"));
                v.setPlantCode(rs.getString("PLANTCODE"));
                v.setAssemblyDate(rs.getTimestamp("ASSEMBLYDATE"));
                v.setShipDate(rs.getTimestamp("SHIPDATE"));
                v.setCurrentMileage(rs.getInt("CURRENTMILEAGE"));
                v.setStatus(rs.getString("STATUS"));
                return v;
            });
    }

    public Map<String, Object> getProductionSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalVehicles", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM VEHICLE", Integer.class));
        summary.put("inProduction", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM VEHICLE WHERE STATUS = 'IN_PRODUCTION'", Integer.class));
        summary.put("assembled", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM VEHICLE WHERE STATUS = 'ASSEMBLED'", Integer.class));
        summary.put("shipped", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM VEHICLE WHERE STATUS = 'SHIPPED'", Integer.class));
        return summary;
    }
}

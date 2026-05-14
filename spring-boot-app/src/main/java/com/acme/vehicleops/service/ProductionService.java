package com.acme.vehicleops.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.acme.vehicleops.model.ProductionStation;
import com.acme.vehicleops.model.ProductionTracking;
import com.acme.vehicleops.model.Vehicle;
import com.acme.vehicleops.repository.ProductionStationJpaRepository;
import com.acme.vehicleops.repository.ProductionTrackingJpaRepository;
import com.acme.vehicleops.repository.VehicleJpaRepository;

/**
 * Production service — extracted from sp_VehicleProductionStatus.
 *
 * Business rules extracted:
 *   - Bottleneck detection: RED if >25% variance, YELLOW if >10%
 *   - Over-cycle-time flag: >1.5x standard cycle time
 *   - Station metrics: avg cycle time, variance, bottleneck flags
 *   - Plant summary: vehicles in production, assembled, shipped counts
 */
@Service
public class ProductionService {

    private static final BigDecimal OVER_CYCLE_MULTIPLIER = new BigDecimal("1.5");

    private final VehicleJpaRepository vehicleRepository;
    private final ProductionStationJpaRepository stationRepository;
    private final ProductionTrackingJpaRepository trackingRepository;

    public ProductionService(VehicleJpaRepository vehicleRepository,
                             ProductionStationJpaRepository stationRepository,
                             ProductionTrackingJpaRepository trackingRepository) {
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.trackingRepository = trackingRepository;
    }

    public List<Vehicle> getVehicles(String plantCode) {
        if (plantCode != null && !plantCode.isEmpty()) {
            return vehicleRepository.findByPlantCodeOrderByAssemblyDateDesc(plantCode);
        }
        return vehicleRepository.findAllByOrderByVehicleIdAsc();
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalVehicles", vehicleRepository.count());
        summary.put("inProduction", vehicleRepository.countByStatus("IN_PRODUCTION"));
        summary.put("assembled", vehicleRepository.countByStatus("ASSEMBLED"));
        summary.put("shipped", vehicleRepository.countByStatus("SHIPPED"));
        return summary;
    }

    public List<Map<String, Object>> getStationMetrics(String plantCode) {
        List<ProductionStation> stations = stationRepository
                .findByPlantCodeOrderBySequenceOrderAsc(plantCode);
        List<Map<String, Object>> metrics = new ArrayList<>();
        for (ProductionStation station : stations) {
            Map<String, Object> m = new HashMap<>();
            m.put("stationCode", station.getStationCode());
            m.put("stationName", station.getStationName());
            m.put("standardCycleTime", station.getCycleTimeMinutes());
            m.put("isQualityGate", station.getIsQualityGate());
            long currentLoad = trackingRepository
                    .countByStationIdAndExitTimeIsNull(station.getStationId());
            m.put("vehiclesInStation", currentLoad);

            Double avgCycleTime = trackingRepository
                    .avgCycleTimeMinutesByStation(station.getStationId());
            BigDecimal avgCycle = avgCycleTime != null
                    ? BigDecimal.valueOf(avgCycleTime) : BigDecimal.ZERO;
            m.put("avgCycleTime", avgCycle);
            m.put("bottleneckFlag", determineBottleneckFlag(avgCycle,
                    station.getCycleTimeMinutes()));
            metrics.add(m);
        }
        return metrics;
    }

    static String determineBottleneckFlag(BigDecimal avgActualCycleTime, BigDecimal stdCycleTime) {
        if (stdCycleTime == null || stdCycleTime.compareTo(BigDecimal.ZERO) == 0) return "GREEN";
        if (avgActualCycleTime == null || avgActualCycleTime.compareTo(BigDecimal.ZERO) == 0) return "GREEN";
        BigDecimal variance = avgActualCycleTime.subtract(stdCycleTime)
                .divide(stdCycleTime, 4, RoundingMode.HALF_UP);
        if (variance.compareTo(new BigDecimal("0.25")) > 0) return "RED";
        if (variance.compareTo(new BigDecimal("0.10")) > 0) return "YELLOW";
        return "GREEN";
    }

    boolean isOverCycleTime(BigDecimal actualMinutes, BigDecimal standardCycleTime) {
        if (actualMinutes == null || standardCycleTime == null) return false;
        BigDecimal threshold = standardCycleTime.multiply(OVER_CYCLE_MULTIPLIER);
        return actualMinutes.compareTo(threshold) > 0;
    }
}

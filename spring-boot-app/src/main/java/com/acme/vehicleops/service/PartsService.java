package com.acme.vehicleops.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.acme.vehicleops.model.Part;
import com.acme.vehicleops.model.PartFitment;
import com.acme.vehicleops.model.PartInventory;
import com.acme.vehicleops.model.Vehicle;
import com.acme.vehicleops.repository.PartFitmentJpaRepository;
import com.acme.vehicleops.repository.PartInventoryJpaRepository;
import com.acme.vehicleops.repository.PartJpaRepository;
import com.acme.vehicleops.repository.VehicleJpaRepository;

/**
 * Parts supersession service — extracted from sp_PartsSupersession.
 *
 * Business rules extracted:
 *   - Supersession chain walking with max depth 10 and loop prevention
 *   - Fitment validation by ModelYear, TrimLevel, EngineCode
 *   - Inventory availability check across warehouses
 */
@Service
public class PartsService {

    private static final int MAX_CHAIN_DEPTH = 10;

    private final PartJpaRepository partRepository;
    private final PartFitmentJpaRepository fitmentRepository;
    private final PartInventoryJpaRepository inventoryRepository;
    private final VehicleJpaRepository vehicleRepository;

    public PartsService(PartJpaRepository partRepository,
                        PartFitmentJpaRepository fitmentRepository,
                        PartInventoryJpaRepository inventoryRepository,
                        VehicleJpaRepository vehicleRepository) {
        this.partRepository = partRepository;
        this.fitmentRepository = fitmentRepository;
        this.inventoryRepository = inventoryRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<Part> getAllParts() {
        return partRepository.findAllByOrderByPartNumberAsc();
    }

    public Map<String, Object> getPartsSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalParts", partRepository.count());
        summary.put("activeParts", partRepository.countByStatus("ACTIVE"));
        summary.put("supersededParts", partRepository.countByStatus("SUPERSEDED"));
        return summary;
    }

    public List<Map<String, Object>> getSupersessionChain(String partNumber) {
        return walkSupersessionChain(partNumber, null);
    }

    public List<Map<String, Object>> getSupersessionChainForVehicle(String partNumber, String vin) {
        Vehicle vehicle = null;
        if (vin != null && !vin.isEmpty()) {
            vehicle = vehicleRepository.findByVin(vin).orElse(null);
        }
        return walkSupersessionChain(partNumber, vehicle);
    }

    List<Map<String, Object>> walkSupersessionChain(String startPartNumber, Vehicle vehicle) {
        List<Map<String, Object>> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String currentPartNumber = startPartNumber;
        int depth = 0;

        while (currentPartNumber != null && depth < MAX_CHAIN_DEPTH) {
            if (visited.contains(currentPartNumber)) {
                break;
            }
            visited.add(currentPartNumber);

            Optional<Part> partOpt = partRepository.findByPartNumber(currentPartNumber);
            if (!partOpt.isPresent()) break;

            Part part = partOpt.get();
            Map<String, Object> entry = new HashMap<>();
            entry.put("depth", depth);
            entry.put("partNumber", part.getPartNumber());
            entry.put("description", part.getDescription());
            entry.put("status", part.getStatus());
            entry.put("unitCost", part.getUnitCost());
            entry.put("listPrice", part.getListPrice());
            entry.put("supersededBy", part.getSupersededByPartNo());

            if (vehicle != null) {
                boolean fits = checkFitment(part.getPartNumber(),
                        vehicle.getModelCode(), vehicle.getModelYear(),
                        vehicle.getTrimLevel(), vehicle.getEngineCode());
                entry.put("fitsVehicle", fits);
            }

            Integer totalInventory = inventoryRepository.sumOnHandQtyByPartNumber(
                    part.getPartNumber());
            entry.put("totalInventory", totalInventory != null ? totalInventory : 0);
            entry.put("inStock", totalInventory != null && totalInventory > 0);

            chain.add(entry);
            currentPartNumber = part.getSupersededByPartNo();
            depth++;
        }
        return chain;
    }

    boolean checkFitment(String partNumber, String modelCode, Integer modelYear,
                         String trimLevel, String engineCode) {
        if (modelCode == null) return false;
        List<PartFitment> fitments = fitmentRepository.findByPartNumberAndModelCode(
                partNumber, modelCode);
        if (fitments.isEmpty()) return false;

        for (PartFitment f : fitments) {
            if (modelYear != null && (modelYear < f.getModelYearFrom()
                    || modelYear > f.getModelYearTo())) {
                continue;
            }
            if (f.getTrimLevel() != null && !f.getTrimLevel().isEmpty()
                    && !f.getTrimLevel().equals(trimLevel)) {
                continue;
            }
            if (f.getEngineCode() != null && !f.getEngineCode().isEmpty()
                    && !f.getEngineCode().equals(engineCode)) {
                continue;
            }
            return true;
        }
        return false;
    }
}

package com.acme.vehicleops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.DealerSettlement;

@Repository
public interface DealerSettlementJpaRepository extends JpaRepository<DealerSettlement, Integer> {

    long countBySettlementBatchNoStartingWith(String batchPrefix);
}

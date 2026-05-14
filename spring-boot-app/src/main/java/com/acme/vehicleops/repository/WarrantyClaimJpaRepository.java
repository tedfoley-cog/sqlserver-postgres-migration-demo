package com.acme.vehicleops.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.WarrantyClaim;

@Repository
public interface WarrantyClaimJpaRepository extends JpaRepository<WarrantyClaim, Integer> {

    List<WarrantyClaim> findAllByOrderByClaimDateDesc(Pageable pageable);

    long countByClaimStatus(String claimStatus);

    @Query("SELECT COALESCE(SUM(w.totalAmount), 0) FROM WarrantyClaim w WHERE w.claimStatus = ?1")
    BigDecimal sumTotalAmountByClaimStatus(String claimStatus);

    List<WarrantyClaim> findByClaimStatusAndSettledDateIsNull(String claimStatus);

    @Query("SELECT COALESCE(MAX(w.claimId), 0) FROM WarrantyClaim w")
    int findMaxClaimId();
}

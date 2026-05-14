package com.acme.vehicleops.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acme.vehicleops.model.Part;

@Repository
public interface PartJpaRepository extends JpaRepository<Part, Integer> {

    Optional<Part> findByPartNumber(String partNumber);

    List<Part> findAllByOrderByPartNumberAsc();

    long countByStatus(String status);
}

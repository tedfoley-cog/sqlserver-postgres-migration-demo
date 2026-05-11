package com.acme.vehicleops.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.vehicleops.model.Part;
import com.acme.vehicleops.repository.PartsRepository;

@RestController
@RequestMapping("/api/parts")
public class PartsController {

    @Autowired
    private PartsRepository partsRepository;

    @GetMapping
    public List<Part> getAllParts() {
        return partsRepository.getAllParts();
    }

    @GetMapping("/supersession")
    public List<Map<String, Object>> getSupersessionChain(
            @RequestParam String partNumber) {
        return partsRepository.getSupersessionChain(partNumber);
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return partsRepository.getPartsSummary();
    }
}

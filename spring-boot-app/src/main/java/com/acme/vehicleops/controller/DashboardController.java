package com.acme.vehicleops.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.acme.vehicleops.service.PartsService;
import com.acme.vehicleops.service.ProductionService;
import com.acme.vehicleops.service.WarrantyService;

@Controller
public class DashboardController {

    @Autowired
    private ProductionService productionService;

    @Autowired
    private WarrantyService warrantyService;

    @Autowired
    private PartsService partsService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("productionSummary", productionService.getSummary());
        model.addAttribute("warrantySummary", warrantyService.getSummary());
        model.addAttribute("partsSummary", partsService.getPartsSummary());
        model.addAttribute("recentClaims", warrantyService.getRecentClaims());
        model.addAttribute("vehicles", productionService.getVehicles(null));
        return "dashboard";
    }
}

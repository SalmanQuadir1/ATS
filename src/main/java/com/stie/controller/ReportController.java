package com.stie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {

    @GetMapping("/reports")
    public String showReports(Model model) {
        model.addAttribute("pageTitle", "Recruitment Analytics");
        model.addAttribute("avgTimeToFill", 18); // Days
        model.addAttribute("offerAcceptanceRate", 85); // %
        model.addAttribute("totalHires", 24);
        model.addAttribute("avgHiringCost", 1250); // S$
        model.addAttribute("totalCostSpent", 30000); // S$
        return "reports";
    }
}


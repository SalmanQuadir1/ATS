package com.stie.controller;

import com.stie.service.BrandingService;
import com.stie.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private JobService jobService;

    @GetMapping("/landing")
    public String landing(Model model) {
        model.addAttribute("branding", brandingService.getBranding());
        model.addAttribute("openJobs", jobService.getOpenJobs());
        return "landing";
    }
}


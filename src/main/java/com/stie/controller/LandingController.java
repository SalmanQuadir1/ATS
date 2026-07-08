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

    @Autowired
    private com.stie.service.TenantService tenantService;

    @GetMapping("/landing")
    public String genericLanding() {
        java.util.List<com.stie.model.Tenant> tenants = tenantService.getAllSites();
        if (!tenants.isEmpty()) {
            return "redirect:/" + tenants.get(0).getSubdomain() + "/landing";
        }
        return "redirect:/login";
    }

    @GetMapping("/{tenantName}/landing")
    public String landing(@org.springframework.web.bind.annotation.PathVariable("tenantName") String tenantName, Model model) {
        com.stie.model.Tenant tenant = tenantService.getSiteBySubdomain(tenantName);
        if (tenant == null) {
            return "redirect:/login"; // or show a 404
        }
        model.addAttribute("tenant", tenant);
        model.addAttribute("branding", brandingService.getBranding(tenant));
        model.addAttribute("openJobs", jobService.getOpenJobsForTenant(tenant));
        return "landing";
    }
}


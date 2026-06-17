package com.stie.controller;

import com.stie.model.BrandingConfig;
import com.stie.model.EmailTemplate;
import com.stie.model.Tenant;
import com.stie.model.User;
import com.stie.service.AuditService;
import com.stie.service.BrandingService;
import com.stie.service.EmailTemplateService;
import com.stie.service.TenantService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SettingController {

    @Autowired
    private AuditService auditService;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {
        model.addAttribute("pageTitle", "System Settings & Access Control");
        model.addAttribute("auditLogs", auditService.getRecentLogs());
        return "settings";
    }

    @GetMapping("/settings/email-templates")
    public String showEmailTemplates(Model model) {
        model.addAttribute("pageTitle", "Recruitment Email Templates Builder");
        model.addAttribute("templates", emailTemplateService.getAllTemplates());
        return "settings-templates";
    }

    @PostMapping("/settings/email-templates")
    public String saveEmailTemplate(
            @RequestParam("templateCode") String templateCode,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            RedirectAttributes redirectAttributes) {

        EmailTemplate template = emailTemplateService.getTemplateByCode(templateCode);
        if (template != null) {
            template.setSubject(subject);
            template.setBody(body);
            emailTemplateService.saveTemplate(template);
            auditService.log("EMAIL_TEMPLATE_UPDATE", getCurrentUser(), "EmailTemplate", template.getId(), 
                "Updated email template: " + template.getTemplateName());
            redirectAttributes.addFlashAttribute("success", "Email template '" + template.getTemplateName() + "' saved successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Email template not found.");
        }
        return "redirect:/settings/email-templates";
    }

    @GetMapping("/settings/branding")
    public String showBranding(Model model) {
        model.addAttribute("pageTitle", "Career Portal Branding Settings");
        model.addAttribute("branding", brandingService.getBranding());
        return "settings-branding";
    }

    @PostMapping("/settings/branding")
    public String saveBranding(
            BrandingConfig brandingConfig,
            RedirectAttributes redirectAttributes) {

        brandingService.saveBranding(brandingConfig);
        auditService.log("BRANDING_UPDATE", getCurrentUser(), "BrandingConfig", 1L, 
            "Updated career portal branding details.");
        redirectAttributes.addFlashAttribute("success", "Career Portal Branding settings saved successfully.");
        return "redirect:/settings/branding";
    }

    @GetMapping("/settings/organization")
    public String showOrganizationConfig(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            model.addAttribute("error", "Your account does not belong to any Site. SuperAdmin/Global users cannot configure site organization.");
            model.addAttribute("pageTitle", "Organization Structure");
            return "settings-org";
        }
        
        model.addAttribute("pageTitle", "Organization Structure");
        model.addAttribute("tenant", user.getTenant());
        return "settings-org";
    }

    @PostMapping("/settings/organization")
    public String saveOrganizationConfig(
            @RequestParam("departmentsText") String departmentsText,
            @RequestParam("locationsText") String locationsText,
            RedirectAttributes redirectAttributes) {

        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            redirectAttributes.addFlashAttribute("error", "Your account does not belong to any Site. SuperAdmin/Global users cannot configure site organization.");
            return "redirect:/settings/organization";
        }

        List<String> departments = Arrays.stream(departmentsText.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        
        List<String> locations = Arrays.stream(locationsText.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        tenantService.updateOrganizationConfig(user.getTenant().getId(), departments, locations);
        
        auditService.log("ORG_UPDATE", getCurrentUser(), "Tenant", user.getTenant().getId(), 
            "Updated organization departments and locations.");
            
        redirectAttributes.addFlashAttribute("success", "Organization structure updated successfully.");
        return "redirect:/settings/organization";
    }
}


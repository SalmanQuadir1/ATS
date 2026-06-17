package com.stie.controller;

import com.stie.model.Tenant;
import com.stie.model.User;
import com.stie.service.AuditService;
import com.stie.service.TenantService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {

    @Autowired private TenantService tenantService;
    @Autowired private UserService userService;
    @Autowired private AuditService auditService;

    private String currentUsername() {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
    }

    @GetMapping
    public String index(@RequestParam(value = "error", required = false) String error, Model model) {
        List<Tenant> sites = tenantService.getAllSites();
        model.addAttribute("pageTitle", "Super Admin — Site Console");
        model.addAttribute("sites", sites);
        model.addAttribute("totalSites", sites.size());
        model.addAttribute("activeSites", tenantService.getActiveSiteCount());
        model.addAttribute("totalUsers", tenantService.getTotalUserCount());
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "super-admin";
    }

    @Autowired private com.stie.repository.RoleRepository roleRepository;

    /** View users of a specific site */
    @GetMapping("/sites/{id}/users")
    public String siteUsers(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Tenant site = tenantService.getSiteById(id).orElse(null);
        if (site == null) {
            ra.addFlashAttribute("error", "Site not found.");
            return "redirect:/super-admin";
        }
        model.addAttribute("pageTitle", "Users — " + site.getName());
        model.addAttribute("site", site);
        model.addAttribute("users", tenantService.getUsersBySite(site));
        model.addAttribute("availableRoles", roleRepository.findByTenantIsNull());
        
        return "super-admin-users";
    }

    /** Create a new site (with optional admin) */
    @PostMapping("/sites/create")
    public String createSite(@RequestParam String name,
                             @RequestParam(required = false, defaultValue = "") String location,
                             @RequestParam(required = false, defaultValue = "") String contactEmail,
                             @RequestParam(required = false, defaultValue = "") String adminUsername,
                             @RequestParam(required = false, defaultValue = "") String adminPassword,
                             @RequestParam(required = false, defaultValue = "") String adminDisplayName,
                             RedirectAttributes ra) {
        try {
            if (!adminUsername.trim().isEmpty() && !adminPassword.trim().isEmpty()) {
                tenantService.createSiteWithAdmin(name, location, contactEmail,
                        adminUsername, adminPassword, adminDisplayName);
                ra.addFlashAttribute("success",
                        "Site '" + name + "' created. Admin account: " + adminUsername);
            } else {
                tenantService.createSite(name, location, contactEmail);
                ra.addFlashAttribute("success", "Site '" + name + "' created successfully.");
            }
            auditService.log("SITE_CREATE", currentUsername(), "Site", null, "Created site: " + name);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin";
    }

    /** Edit site details */
    @PostMapping("/sites/{id}/edit")
    public String editSite(@PathVariable Long id,
                           @RequestParam String name,
                           @RequestParam(required = false, defaultValue = "") String location,
                           @RequestParam(required = false, defaultValue = "") String contactEmail,
                           RedirectAttributes ra) {
        try {
            tenantService.updateSite(id, name, location, contactEmail);
            ra.addFlashAttribute("success", "Site updated successfully.");
            auditService.log("SITE_UPDATE", currentUsername(), "Site", id, "Updated: " + name);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin";
    }

    @PostMapping("/sites/{id}/suspend")
    public String suspendSite(@PathVariable Long id, RedirectAttributes ra) {
        tenantService.suspendSite(id);
        ra.addFlashAttribute("success", "Site suspended.");
        auditService.log("SITE_SUSPEND", currentUsername(), "Site", id, "Suspended");
        return "redirect:/super-admin";
    }

    @PostMapping("/sites/{id}/resume")
    public String resumeSite(@PathVariable Long id, RedirectAttributes ra) {
        tenantService.resumeSite(id);
        ra.addFlashAttribute("success", "Site reactivated.");
        auditService.log("SITE_RESUME", currentUsername(), "Site", id, "Reactivated");
        return "redirect:/super-admin";
    }

    @PostMapping("/sites/{id}/delete")
    public String deleteSite(@PathVariable Long id, RedirectAttributes ra) {
        try {
            tenantService.deleteSite(id);
            ra.addFlashAttribute("success", "Site deleted.");
            auditService.log("SITE_DELETE", currentUsername(), "Site", id, "Deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot delete site: " + e.getMessage());
        }
        return "redirect:/super-admin";
    }

    /** Add a user to a site (called from super-admin-users page) */
    @PostMapping("/sites/{id}/users/add")
    public String addUserToSite(@PathVariable Long id,
                                @RequestParam String username,
                                @RequestParam String password,
                                @RequestParam Long roleId,
                                @RequestParam(required = false, defaultValue = "") String displayName,
                                @RequestParam(required = false, defaultValue = "") String email,
                                RedirectAttributes ra) {
        Tenant site = tenantService.getSiteById(id).orElse(null);
        if (site == null) {
            ra.addFlashAttribute("error", "Site not found.");
            return "redirect:/super-admin";
        }
        try {
            userService.createUserForSite(site, username, password, roleId, displayName, email);
            ra.addFlashAttribute("success", "User '" + username + "' added to " + site.getName());
            auditService.log("USER_CREATE", currentUsername(), "User", null,
                    "Created user '" + username + "' with role ID: " + roleId + " for site: " + site.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/sites/" + id + "/users";
    }

    /** Delete a user from a site */
    @PostMapping("/sites/{siteId}/users/{userId}/delete")
    public String deleteUser(@PathVariable Long siteId, @PathVariable Long userId, RedirectAttributes ra) {
        try {
            userService.deleteUser(userId);
            ra.addFlashAttribute("success", "User deleted.");
            auditService.log("USER_DELETE", currentUsername(), "User", userId, "Deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/super-admin/sites/" + siteId + "/users";
    }
}


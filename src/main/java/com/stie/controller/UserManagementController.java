package com.stie.controller;

import com.stie.model.Tenant;
import com.stie.model.User;
import com.stie.service.AuditService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserManagementController {

    @Autowired private UserService userService;
    @Autowired private AuditService auditService;

    @Autowired private com.stie.repository.RoleRepository roleRepository;

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Site Admin: Register a new user within their own site */
    @GetMapping("/users/register")
    public String showRegisterForm(Model model) {
        User current = userService.getCurrentUser();
        if (current == null) return "redirect:/login";

        model.addAttribute("pageTitle", "Register New Team Member");
        model.addAttribute("currentSite", current.getTenant());
        model.addAttribute("siteUsers", current.getTenant() != null
                ? userService.getUsersBySite(current.getTenant()) : java.util.Collections.emptyList());
        java.util.List<com.stie.model.Role> roles = current.getTenant() != null ? roleRepository.findByTenantOrTenantIsNull(current.getTenant()) : roleRepository.findByTenantIsNull();
        roles = roles.stream().filter(r -> !com.stie.config.AppConstants.Roles.SUPER_ADMIN.equals(r.getName()) && !r.getName().toLowerCase().contains("superadmin") && !r.getName().toLowerCase().contains("super_admin")).collect(java.util.stream.Collectors.toList());
        model.addAttribute("availableRoles", roles);
        
        return "register";
    }

    @PostMapping("/users/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam Long roleId,
                               @RequestParam String displayName,
                               @RequestParam String email,
                               RedirectAttributes ra, Model model) {
        User current = userService.getCurrentUser();
        if (current == null) return "redirect:/login";
        Tenant site = current.getTenant();

        if (userService.findByUsername(username) != null) {
            model.addAttribute("error", "Username '" + username + "' already exists.");
            model.addAttribute("pageTitle", "Register New Team Member");
            model.addAttribute("currentSite", site);
            model.addAttribute("siteUsers", site != null ? userService.getUsersBySite(site) : java.util.Collections.emptyList());
            java.util.List<com.stie.model.Role> roles = current.getTenant() != null ? roleRepository.findByTenantOrTenantIsNull(current.getTenant()) : roleRepository.findByTenantIsNull();
            roles = roles.stream().filter(r -> !com.stie.config.AppConstants.Roles.SUPER_ADMIN.equals(r.getName()) && !r.getName().toLowerCase().contains("superadmin") && !r.getName().toLowerCase().contains("super_admin")).collect(java.util.stream.Collectors.toList());
            model.addAttribute("availableRoles", roles);
            return "register";
        }

        try {
            userService.createUserForSite(site, username, password, roleId, displayName, email);
            auditService.log("USER_REGISTER", currentUsername(), "User", null,
                    "Registered user with role ID: " + roleId + ": " + username);
            ra.addFlashAttribute("success", "User '" + username + "' registered successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/register";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User current = userService.getCurrentUser();
            User target = userService.findById(id);
            if (target == null) {
                ra.addFlashAttribute("error", "User not found.");
                return "redirect:/users/register";
            }
            if (current == null || current.getTenant() == null
                    || !current.getTenant().equals(target.getTenant())) {
                ra.addFlashAttribute("error", "You can only delete users from your own site.");
                return "redirect:/users/register";
            }
            userService.deleteUser(id);
            auditService.log("USER_DELETE", currentUsername(), "User", id, "Deleted by site admin");
            ra.addFlashAttribute("success", "User deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/users/register";
    }

    @GetMapping("/users/password")
    public String showPasswordChangeForm(Model model) {
        model.addAttribute("pageTitle", "Change Password");
        return "password-change";
    }

    @PostMapping("/users/password")
    public String changePassword(@RequestParam String newPassword,
                                 @RequestParam String confirmPassword, Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "password-change";
        }
        userService.updatePassword(currentUsername(), newPassword);
        auditService.log("PASSWORD_CHANGE", currentUsername(), "User", null, "Password changed");
        model.addAttribute("success", "Password updated successfully!");
        return "password-change";
    }
}


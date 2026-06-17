package com.stie.controller;

import com.stie.model.Role;
import com.stie.model.User;
import com.stie.repository.PermissionModuleRepository;
import com.stie.repository.RoleRepository;
import com.stie.service.AuditService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/settings/roles")
@PreAuthorize("hasAuthority('MANAGE_ROLES') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
public class TenantRoleController {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionModuleRepository permissionModuleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    @GetMapping
    public String listTenantRoles(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        if (user.getTenant() == null) {
            // SuperAdmin or Global user - direct to super admin roles
            return "redirect:/super-admin/roles";
        }

        model.addAttribute("pageTitle", "Role Management");
        model.addAttribute("roles", roleRepository.findByTenantOrTenantIsNull(user.getTenant()));
        model.addAttribute("allPermissions", permissionModuleRepository.findAll());
        return "settings-roles";
    }

    @PostMapping("/create")
    public String createRole(@RequestParam String name,
                             @RequestParam(required = false) List<String> permissions,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null || user.getTenant() == null) return "redirect:/login";

        // Check if role name exists for this tenant
        boolean exists = roleRepository.findByTenant(user.getTenant()).stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(name));
        
        if (exists) {
            redirectAttributes.addFlashAttribute("error", "Role name already exists.");
            return "redirect:/settings/roles";
        }

        Set<String> perms = new HashSet<>();
        if (permissions != null) {
            perms = new HashSet<>(permissions);
        }

        Role role = new Role(name, perms, false);
        role.setTenant(user.getTenant());
        roleRepository.save(role);

        auditService.log("ROLE_CREATE", user.getUsername(), "Role", role.getId(), 
            "Created custom role: " + name, user.getTenant());

        redirectAttributes.addFlashAttribute("success", "Custom role created successfully.");
        return "redirect:/settings/roles";
    }

    @PostMapping("/update/{id}")
    public String updateRole(@PathVariable Long id,
                             @RequestParam(required = false) List<String> permissions,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null || user.getTenant() == null) return "redirect:/login";

        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        // Ensure role belongs to this tenant and is not a system role
        if (role.getTenant() == null || !role.getTenant().getId().equals(user.getTenant().getId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to modify this role.");
            return "redirect:/settings/roles";
        }
        
        if (role.isSystemRole()) {
            redirectAttributes.addFlashAttribute("error", "System roles cannot be modified.");
            return "redirect:/settings/roles";
        }

        Set<String> perms = new HashSet<>();
        if (permissions != null) {
            perms = new HashSet<>(permissions);
        }
        role.setPermissions(perms);
        roleRepository.save(role);

        auditService.log("ROLE_UPDATE", user.getUsername(), "Role", role.getId(), 
            "Updated custom role: " + role.getName(), user.getTenant());

        redirectAttributes.addFlashAttribute("success", "Permissions for '" + role.getName() + "' updated successfully.");
        return "redirect:/settings/roles";
    }

    @PostMapping("/delete/{id}")
    public String deleteRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null || user.getTenant() == null) return "redirect:/login";

        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        
        // Ensure role belongs to this tenant and is not a system role
        if (role.getTenant() == null || !role.getTenant().getId().equals(user.getTenant().getId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to delete this role.");
            return "redirect:/settings/roles";
        }
        
        if (role.isSystemRole()) {
            redirectAttributes.addFlashAttribute("error", "System roles cannot be deleted.");
            return "redirect:/settings/roles";
        }

        // Before deleting, ensure no users are assigned to it (optional but good practice)
        // Since we don't have user Role management tightly coupled here, we will just delete it.
        // Spring Data JPA might throw DataIntegrityViolationException if foreign key constraint fails.
        try {
            roleRepository.delete(role);
            auditService.log("ROLE_DELETE", user.getUsername(), "Role", role.getId(), 
                "Deleted custom role: " + role.getName(), user.getTenant());
            redirectAttributes.addFlashAttribute("success", "Custom role deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete role. It might be assigned to users.");
        }
        
        return "redirect:/settings/roles";
    }
}

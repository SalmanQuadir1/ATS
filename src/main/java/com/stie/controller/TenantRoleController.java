package com.stie.controller;

import com.stie.model.PermissionModule;
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
@PreAuthorize("hasAuthority('MANAGE_ROLES') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
public class TenantRoleController {

    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionModuleRepository permissionModuleRepository;
    @Autowired private UserService userService;
    @Autowired private AuditService auditService;

    // ─── Roles ─────────────────────────────────────────────────────────────

    @GetMapping("/settings/roles")
    public String listTenantRoles(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        if (user.getTenant() == null) {
            return "redirect:/super-admin/roles";
        }

        model.addAttribute("pageTitle", "Roles & Modules");
        model.addAttribute("roles", roleRepository.findByTenantOrTenantIsNull(user.getTenant()));
        model.addAttribute("allPermissions", permissionModuleRepository.findAll());
        return "settings-roles";
    }

    @PostMapping("/settings/roles/create")
    public String createRole(@RequestParam String name,
                             @RequestParam(required = false) List<String> permissions,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null || user.getTenant() == null) return "redirect:/login";

        boolean exists = roleRepository.findByTenant(user.getTenant()).stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(name));
        if (exists) {
            redirectAttributes.addFlashAttribute("error", "Role name already exists.");
            return "redirect:/settings/roles";
        }

        Set<String> perms = new HashSet<>();
        if (permissions != null) perms = new HashSet<>(permissions);

        Role role = new Role(name, perms, false);
        role.setTenant(user.getTenant());
        roleRepository.save(role);

        auditService.log("ROLE_CREATE", user.getUsername(), "Role", role.getId(),
            "Created custom role: " + name, user.getTenant());

        redirectAttributes.addFlashAttribute("success", "Custom role created successfully.");
        return "redirect:/settings/roles";
    }

    @PostMapping("/settings/roles/update/{id}")
    public String updateRole(@PathVariable Long id,
                             @RequestParam(required = false) List<String> permissions,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null || user.getTenant() == null) return "redirect:/login";

        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        if (role.getTenant() == null || !role.getTenant().getId().equals(user.getTenant().getId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to modify this role.");
            return "redirect:/settings/roles";
        }
        if (role.isSystemRole()) {
            redirectAttributes.addFlashAttribute("error", "System roles cannot be modified.");
            return "redirect:/settings/roles";
        }

        Set<String> perms = new HashSet<>();
        if (permissions != null) perms = new HashSet<>(permissions);
        role.setPermissions(perms);
        roleRepository.save(role);

        auditService.log("ROLE_UPDATE", user.getUsername(), "Role", role.getId(),
            "Updated custom role: " + role.getName(), user.getTenant());

        redirectAttributes.addFlashAttribute("success", "Permissions for '" + role.getName() + "' updated successfully.");
        return "redirect:/settings/roles";
    }

    @PostMapping("/settings/roles/delete/{id}")
    public String deleteRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null || user.getTenant() == null) return "redirect:/login";

        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        if (role.getTenant() == null || !role.getTenant().getId().equals(user.getTenant().getId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to delete this role.");
            return "redirect:/settings/roles";
        }
        if (role.isSystemRole()) {
            redirectAttributes.addFlashAttribute("error", "System roles cannot be deleted.");
            return "redirect:/settings/roles";
        }

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

    // ─── Permission Modules CRUD ────────────────────────────────────────────

    @PostMapping("/settings/modules/create")
    public String createModule(@RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String isNavItem,
                               @RequestParam(required = false) String navLabel,
                               @RequestParam(required = false) String navUrl,
                               @RequestParam(required = false, defaultValue = "main") String navGroup,
                               @RequestParam(required = false, defaultValue = "100") int navOrder,
                               RedirectAttributes redirectAttributes) {
        String safeName = name.toUpperCase().replace(" ", "_");
        if (permissionModuleRepository.findByName(safeName).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Module '" + safeName + "' already exists.");
            return "redirect:/settings/roles";
        }

        PermissionModule pm = new PermissionModule(safeName, description);
        pm.setNavItem("true".equals(isNavItem));
        pm.setNavLabel(navLabel);
        pm.setNavUrl(navUrl);
        pm.setNavGroup(navGroup != null ? navGroup : "main");
        pm.setNavOrder(navOrder);
        pm.setSystemModule(false);
        permissionModuleRepository.save(pm);

        redirectAttributes.addFlashAttribute("success", "Module '" + safeName + "' created successfully.");
        return "redirect:/settings/roles";
    }

    @PostMapping("/settings/modules/update/{id}")
    public String updateModule(@PathVariable Long id,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String isNavItem,
                               @RequestParam(required = false) String navLabel,
                               @RequestParam(required = false) String navUrl,
                               @RequestParam(required = false, defaultValue = "main") String navGroup,
                               @RequestParam(required = false, defaultValue = "0") int navOrder,
                               RedirectAttributes redirectAttributes) {
        PermissionModule pm = permissionModuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + id));

        pm.setDescription(description);
        pm.setNavItem("true".equals(isNavItem));
        pm.setNavLabel(navLabel);
        pm.setNavUrl(navUrl);
        pm.setNavGroup(navGroup != null ? navGroup : "main");
        pm.setNavOrder(navOrder);
        permissionModuleRepository.save(pm);

        redirectAttributes.addFlashAttribute("success", "Module '" + pm.getName() + "' updated.");
        return "redirect:/settings/roles";
    }

    @PostMapping("/settings/modules/delete/{id}")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        PermissionModule pm = permissionModuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + id));

        if (pm.isSystemModule()) {
            redirectAttributes.addFlashAttribute("error", "Built-in system modules cannot be deleted.");
            return "redirect:/settings/roles";
        }

        permissionModuleRepository.delete(pm);
        redirectAttributes.addFlashAttribute("success", "Module deleted.");
        return "redirect:/settings/roles";
    }
}

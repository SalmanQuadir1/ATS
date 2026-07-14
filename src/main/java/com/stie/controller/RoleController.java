package com.stie.controller;

import com.stie.model.PermissionModule;
import com.stie.model.Role;
import com.stie.repository.PermissionModuleRepository;
import com.stie.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/super-admin/roles")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionModuleRepository permissionModuleRepository;

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleRepository.findByTenantIsNull());
        model.addAttribute("allPermissions", permissionModuleRepository.findAll());
        return "super-admin-roles";
    }

    @PostMapping("/create")
    public String createRole(@RequestParam String name,
                             @RequestParam(required = false) List<String> permissions,
                             RedirectAttributes redirectAttributes) {
        if (!roleRepository.findByNameAndTenantIsNull(name).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Role already exists.");
            return "redirect:/super-admin/roles";
        }

        Set<String> perms = new HashSet<>();
        if (permissions != null) {
            perms = new HashSet<>(permissions);
        }

        Role role = new Role(name, perms, false);
        roleRepository.save(role);
        redirectAttributes.addFlashAttribute("success", "Role created successfully.");
        return "redirect:/super-admin/roles";
    }

    @PostMapping("/update/{id}")
    public String updateRole(@PathVariable Long id,
                             @RequestParam(required = false) List<String> permissions,
                             RedirectAttributes redirectAttributes) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        Set<String> perms = new HashSet<>();
        if (permissions != null) {
            perms = new HashSet<>(permissions);
        }
        role.setPermissions(perms);
        roleRepository.save(role);

        redirectAttributes.addFlashAttribute("success", "Permissions for '" + role.getName() + "' updated successfully.");
        return "redirect:/super-admin/roles";
    }

    @PostMapping("/delete/{id}")
    public String deleteRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        if (role.isSystemRole()) {
            redirectAttributes.addFlashAttribute("error", "System roles cannot be deleted.");
            return "redirect:/super-admin/roles";
        }
        roleRepository.delete(role);
        redirectAttributes.addFlashAttribute("success", "Role deleted successfully.");
        return "redirect:/super-admin/roles";
    }

    @PostMapping("/module/create")
    public String createModule(@RequestParam String name,
                               @RequestParam String description,
                               @RequestParam(required = false) boolean generateCrud,
                               RedirectAttributes redirectAttributes) {
        String safeName = name.toUpperCase().replace(" ", "_");
        
        if (generateCrud) {
            String[] crud = {"CREATE", "READ", "UPDATE", "DELETE"};
            boolean addedAny = false;
            for(String op : crud) {
                String opName = op + "_" + safeName;
                if (!permissionModuleRepository.findByName(opName).isPresent()) {
                    permissionModuleRepository.save(new PermissionModule(opName, "Can " + op.toLowerCase() + " " + description.toLowerCase()));
                    addedAny = true;
                }
            }
            if (addedAny) {
                redirectAttributes.addFlashAttribute("success", "CRUD Permission Modules for '" + safeName + "' created successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "CRUD modules for '" + safeName + "' already exist.");
            }
        } else {
            if (permissionModuleRepository.findByName(safeName).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Permission Module already exists.");
                return "redirect:/super-admin/roles";
            }
            permissionModuleRepository.save(new PermissionModule(safeName, description));
            redirectAttributes.addFlashAttribute("success", "Permission Module created successfully.");
        }
        return "redirect:/super-admin/roles";
    }
}

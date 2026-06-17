package com.stie.controller;

import com.stie.model.Department;
import com.stie.model.User;
import com.stie.service.AuditService;
import com.stie.service.DepartmentService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/departments")
    public String list(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            model.addAttribute("error", "Your account does not belong to any Site. SuperAdmin/Global users cannot manage site-specific departments.");
            model.addAttribute("pageTitle", "Departments");
            model.addAttribute("departments", new java.util.ArrayList<>());
            return "departments";
        }
        java.util.List<Department> depts = departmentService.getDepartmentsByTenant(user.getTenant());
        java.util.List<String> oldDepts = user.getTenant().getDepartments();
        if (depts.isEmpty() && oldDepts != null && !oldDepts.isEmpty()) {
            for (String name : oldDepts) {
                if (name != null && !name.trim().isEmpty())
                    departmentService.create(name.trim(), user.getTenant());
            }
            depts = departmentService.getDepartmentsByTenant(user.getTenant());
        }
        model.addAttribute("pageTitle", "Departments");
        model.addAttribute("departments", depts);
        return "departments";
    }

    @GetMapping("/departments/new")
    public String showCreateForm(Model model) {
        model.addAttribute("pageTitle", "New Department");
        model.addAttribute("department", new Department());
        return "department-form";
    }

    @PostMapping("/departments/create")
    public String create(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            redirectAttributes.addFlashAttribute("error", "Your account does not belong to any Site. SuperAdmin/Global users cannot manage site-specific departments.");
            return "redirect:/departments";
        }
        Department saved = departmentService.create(name, user.getTenant());
        auditService.log("DEPARTMENT_CREATE", getCurrentUser(), "Department", saved.getId(), "Name: " + saved.getName());
        redirectAttributes.addFlashAttribute("success", "Department '" + saved.getName() + "' created.");
        return "redirect:/departments";
    }

    @GetMapping("/departments/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Department dept = departmentService.getById(id).orElse(null);
        if (dept == null) return "redirect:/departments";
        model.addAttribute("pageTitle", "Edit Department");
        model.addAttribute("department", dept);
        return "department-form";
    }

    @PostMapping("/departments/{id}/update")
    public String update(@PathVariable Long id, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        Department updated = departmentService.update(id, name);
        auditService.log("DEPARTMENT_UPDATE", getCurrentUser(), "Department", id, "Name: " + updated.getName());
        redirectAttributes.addFlashAttribute("success", "Department updated.");
        return "redirect:/departments";
    }

    @PostMapping("/departments/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        departmentService.delete(id);
        auditService.log("DEPARTMENT_DELETE", getCurrentUser(), "Department", id, "Deleted");
        redirectAttributes.addFlashAttribute("success", "Department deleted.");
        return "redirect:/departments";
    }
}


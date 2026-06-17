package com.stie.controller;

import com.stie.model.JobCategory;
import com.stie.model.User;
import com.stie.service.AuditService;
import com.stie.service.JobCategoryService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/settings/categories")
public class JobCategoryController {

    @Autowired
    private JobCategoryService categoryService;

    @Autowired
    private com.stie.repository.JobCategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    @GetMapping
    public String listCategories(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            model.addAttribute("error", "Your account does not belong to any Site. SuperAdmin/Global users cannot manage categories.");
            model.addAttribute("pageTitle", "Job Categories");
            model.addAttribute("categories", new java.util.ArrayList<>());
            return "settings-categories";
        }

        model.addAttribute("pageTitle", "Job Categories");
        model.addAttribute("categories", categoryRepository.findByTenantWithSkills(user.getTenant()));
        return "settings-categories";
    }

    @PostMapping("/create")
    public String createCategory(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            redirectAttributes.addFlashAttribute("error", "Global users cannot manage categories.");
            return "redirect:/settings/categories";
        }

        JobCategory category = categoryService.createCategory(name, user.getTenant());
        auditService.log("CATEGORY_CREATE", user.getUsername(), "JobCategory", category.getId(), "Name: " + category.getName());
        redirectAttributes.addFlashAttribute("success", "Job Category '" + category.getName() + "' created.");
        return "redirect:/settings/categories";
    }
    @PostMapping("/{id}/update")
    public String updateCategory(@PathVariable("id") Long id, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        Optional<JobCategory> opt = categoryService.getCategoryById(id);
        if (opt.isPresent() && opt.get().getTenant().equals(user.getTenant())) {
            JobCategory updated = categoryService.updateCategory(id, name);
            auditService.log("CATEGORY_UPDATE", user.getUsername(), "JobCategory", id, "Updated to: " + updated.getName());
            redirectAttributes.addFlashAttribute("success", "Category updated successfully.");
        }
        return "redirect:/settings/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        Optional<JobCategory> opt = categoryService.getCategoryById(id);
        if (opt.isPresent() && opt.get().getTenant().equals(user.getTenant())) {
            categoryService.deleteCategory(id);
            auditService.log("CATEGORY_DELETE", user.getUsername(), "JobCategory", id, "Deleted " + opt.get().getName());
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully.");
        }
        return "redirect:/settings/categories";
    }

    @GetMapping("/{id}/skills")
    public String manageSkills(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        Optional<JobCategory> opt = categoryService.getCategoryById(id);
        if (!opt.isPresent() || !opt.get().getTenant().equals(user.getTenant())) {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            return "redirect:/settings/categories";
        }

        model.addAttribute("pageTitle", "Skills for " + opt.get().getName());
        model.addAttribute("category", opt.get());
        model.addAttribute("skills", categoryService.getSkillsByCategory(opt.get()));
        return "settings-category-skills";
    }

    @PostMapping("/{id}/skills/add")
    public String addSkill(@PathVariable("id") Long id, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        Optional<JobCategory> opt = categoryService.getCategoryById(id);
        if (opt.isPresent() && opt.get().getTenant().equals(user.getTenant())) {
            categoryService.addSkill(name, opt.get());
            redirectAttributes.addFlashAttribute("success", "Skill added.");
        }
        return "redirect:/settings/categories/" + id + "/skills";
    }
    @PostMapping("/{categoryId}/skills/{skillId}/update")
    public String updateSkill(@PathVariable("categoryId") Long categoryId, @PathVariable("skillId") Long skillId, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        Optional<JobCategory> opt = categoryService.getCategoryById(categoryId);
        if (opt.isPresent() && opt.get().getTenant().equals(user.getTenant())) {
            categoryService.updateSkill(skillId, name);
            redirectAttributes.addFlashAttribute("success", "Skill updated.");
        }
        return "redirect:/settings/categories/" + categoryId + "/skills";
    }

    @PostMapping("/{categoryId}/skills/{skillId}/delete")
    public String deleteSkill(@PathVariable("categoryId") Long categoryId, @PathVariable("skillId") Long skillId, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        Optional<JobCategory> opt = categoryService.getCategoryById(categoryId);
        if (opt.isPresent() && opt.get().getTenant().equals(user.getTenant())) {
            categoryService.deleteSkill(skillId);
            redirectAttributes.addFlashAttribute("success", "Skill deleted.");
        }
        return "redirect:/settings/categories/" + categoryId + "/skills";
    }
}


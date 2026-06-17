package com.stie.controller;

import com.stie.model.JobVacancy;
import com.stie.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;
import com.stie.service.JobDistributionService;
import com.stie.service.DepartmentService;
import com.stie.service.LocationService;
import com.stie.service.JobCategoryService;
import com.stie.model.JobCategory;
import com.stie.model.Skill;
import com.stie.repository.CandidateRepository;

@Controller
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private com.stie.service.AuditService auditService;

    @Autowired
    private JobDistributionService distributionService;

    @Autowired
    private com.stie.service.UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private JobCategoryService categoryService;

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/jobs")
    public String listJobs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) String status,
                           Model model) {
        model.addAttribute("pageTitle", "Jobs");
        java.util.List<JobVacancy> all = jobService.getAllVacancies();
        // Filter by search
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            all = all.stream().filter(j -> (j.getTitle() != null && j.getTitle().toLowerCase().contains(q))
                || (j.getDepartment() != null && j.getDepartment().toLowerCase().contains(q))
                || (j.getLocation() != null && j.getLocation().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        }
        // Filter by status
        if (status != null && !status.isEmpty()) {
            final String s = status;
            all = all.stream().filter(j -> j.getStatus() != null && j.getStatus().name().equalsIgnoreCase(s))
                .collect(Collectors.toList());
        }
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = page * size;
        int end = Math.min(start + size, total);
        java.util.List<JobVacancy> paged = start < total ? all.subList(start, end) : java.util.Collections.emptyList();
        model.addAttribute("jobs", paged);
        model.addAttribute("totalJobs", total);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        return "jobs";
    }

    @GetMapping("/jobs/new")
    public String showCreateForm(Model model) {
        com.stie.model.User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("pageTitle", "Create New Job");
        model.addAttribute("vacancy", new JobVacancy());
        model.addAttribute("existingSkillIds", java.util.Collections.emptyList());
        
        if (user.getTenant() != null) {
            List<String> depts = departmentService.getDepartmentsByTenant(user.getTenant()).stream()
                    .map(d -> d.getName()).collect(Collectors.toList());
            List<String> locs = locationService.getLocationsByTenant(user.getTenant()).stream()
                    .map(l -> l.getName()).collect(Collectors.toList());
            if (depts.isEmpty()) depts = user.getTenant().getDepartments();
            if (locs.isEmpty()) locs = user.getTenant().getLocations();
            model.addAttribute("departments", depts);
            model.addAttribute("locations", locs);
            model.addAttribute("categories", categoryService.getCategoriesByTenant(user.getTenant()));
        }
        return "job-form";
    }

    @GetMapping("/jobs/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        JobVacancy vacancy = jobService.getAllVacancies().stream()
                .filter(j -> j.getId().equals(id)).findFirst().orElse(null);
        if (vacancy == null) {
            redirectAttributes.addFlashAttribute("error", "Job not found.");
            return "redirect:/jobs";
        }
        model.addAttribute("pageTitle", "Edit Job");
        model.addAttribute("vacancy", vacancy);
        model.addAttribute("existingSkillIds", vacancy.getSkills().stream().map(Skill::getId).collect(Collectors.toList()));
        com.stie.model.User user = userService.getCurrentUser();
        if (user != null && user.getTenant() != null) {
            List<String> depts = departmentService.getDepartmentsByTenant(user.getTenant()).stream()
                    .map(d -> d.getName()).collect(Collectors.toList());
            List<String> locs = locationService.getLocationsByTenant(user.getTenant()).stream()
                    .map(l -> l.getName()).collect(Collectors.toList());
            if (depts.isEmpty()) depts = user.getTenant().getDepartments();
            if (locs.isEmpty()) locs = user.getTenant().getLocations();
            model.addAttribute("departments", depts);
            model.addAttribute("locations", locs);
            model.addAttribute("categories", categoryService.getCategoriesByTenant(user.getTenant()));
        }
        return "job-form";
    }

    @PostMapping("/jobs/create")
    public String createJob(JobVacancy vacancy,
                            @RequestParam(value = "location", required = false, defaultValue = "") String location,
                            @RequestParam(value = "categoryId", required = false) Long categoryId,
                            @RequestParam(value = "skillIds", required = false) List<Long> skillIds,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        vacancy.setLocation(location);
        
        if (categoryId != null) {
            categoryService.getCategoryById(categoryId).ifPresent(vacancy::setCategory);
        }
        
        if (skillIds != null && !skillIds.isEmpty() && vacancy.getCategory() != null) {
            java.util.Set<Skill> selectedSkills = new java.util.HashSet<>();
            for (Skill s : categoryService.getSkillsByCategory(vacancy.getCategory())) {
                if (skillIds.contains(s.getId())) {
                    selectedSkills.add(s);
                }
            }
            vacancy.setSkills(selectedSkills);
        }

        JobVacancy saved = jobService.saveVacancy(vacancy);
        auditService.log("JOB_CREATE", getCurrentUser(), "JobVacancy", saved.getId(), "Title: " + saved.getTitle());
        redirectAttributes.addFlashAttribute("success", "Job created successfully!");
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/update")
    public String updateJob(JobVacancy vacancy,
                            @RequestParam(value = "location", required = false, defaultValue = "") String location,
                            @RequestParam(value = "categoryId", required = false) Long categoryId,
                            @RequestParam(value = "skillIds", required = false) List<Long> skillIds,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        JobVacancy existing = jobService.getAllVacancies().stream()
                .filter(j -> j.getId().equals(vacancy.getId())).findFirst().orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Job not found.");
            return "redirect:/jobs";
        }
        existing.setTitle(vacancy.getTitle());
        existing.setDescription(vacancy.getDescription());
        existing.setDepartment(vacancy.getDepartment());
        existing.setLocation(location);
        existing.setExpiryDate(vacancy.getExpiryDate());
        existing.setNoOfPosts(vacancy.getNoOfPosts());

        if (categoryId != null) {
            categoryService.getCategoryById(categoryId).ifPresent(existing::setCategory);
        }
        if (skillIds != null && !skillIds.isEmpty() && existing.getCategory() != null) {
            java.util.Set<Skill> selectedSkills = new java.util.HashSet<>();
            for (Skill s : categoryService.getSkillsByCategory(existing.getCategory())) {
                if (skillIds.contains(s.getId())) {
                    selectedSkills.add(s);
                }
            }
            existing.setSkills(selectedSkills);
        } else {
            existing.setSkills(new java.util.HashSet<>());
        }

        jobService.saveVacancy(existing);
        auditService.log("JOB_UPDATE", getCurrentUser(), "JobVacancy", existing.getId(), "Title: " + existing.getTitle());
        redirectAttributes.addFlashAttribute("success", "Job updated successfully!");
        return "redirect:/jobs";
    }

    @GetMapping("/jobs/{id}")
    public String viewJob(@PathVariable Long id, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        JobVacancy job = jobService.getAllVacancies().stream()
                .filter(j -> j.getId().equals(id)).findFirst().orElse(null);
        if (job == null) {
            redirectAttributes.addFlashAttribute("error", "Job not found.");
            return "redirect:/jobs";
        }
        model.addAttribute("pageTitle", "Job Details");
        model.addAttribute("job", job);
        model.addAttribute("totalApplicants", candidateRepository.countByJobVacancy(job));
        model.addAttribute("shortlistedCount", candidateRepository.countByJobVacancyAndStatusIn(job, java.util.Arrays.asList(
                com.stie.model.Candidate.CandidateStatus.SHORTLISTED,
                com.stie.model.Candidate.CandidateStatus.INTERVIEW,
                com.stie.model.Candidate.CandidateStatus.OFFERED,
                com.stie.model.Candidate.CandidateStatus.HIRED
        )));
        return "job-detail";
    }

    @GetMapping("/jobs/pending")
    public String listPending(Model model) {
        model.addAttribute("pageTitle", "Pending Approvals");
        model.addAttribute("jobs", jobService.getPendingJobs());
        return "jobs";
    }

    @GetMapping("/jobs/approvals")
    public String approvals(Model model) {
        com.stie.model.User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        
        List<JobVacancy> pendingJobs;
        if ("ROLE_MANAGER".equals(user.getRole())) {
            pendingJobs = jobService.getPendingJobs().stream()
                    .filter(j -> j.getHiringManager() != null && j.getHiringManager().getId().equals(user.getId()))
                    .collect(java.util.stream.Collectors.toList());
            if (pendingJobs.isEmpty()) {
                pendingJobs = jobService.getPendingJobs();
            }
        } else {
            pendingJobs = jobService.getPendingJobs();
        }
        
        model.addAttribute("pageTitle", "Requisition Approvals Dashboard");
        model.addAttribute("jobs", pendingJobs);
        model.addAttribute("currentUser", user);
        
        return "jobs-approvals";
    }

    @PostMapping("/jobs/{id}/approve")
    public String approve(@PathVariable Long id, 
                           @RequestParam(value = "note", required = false, defaultValue = "Approved") String note,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        jobService.approveVacancy(id, note, getCurrentUser());
        redirectAttributes.addFlashAttribute("success", "Job vacancy approved successfully!");
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/{id}/reject")
    public String reject(@PathVariable Long id, 
                          @RequestParam(value = "note", required = false, defaultValue = "Rejected") String note,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        jobService.rejectVacancy(id, note, getCurrentUser());
        redirectAttributes.addFlashAttribute("success", "Job vacancy rejected successfully.");
        return "redirect:/jobs";
    }
    @PostMapping("/jobs/{id}/distribute")
    public String distributeJob(@PathVariable Long id, 
                                @RequestParam(required = false) List<String> portals,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        JobVacancy job = jobService.getAllVacancies().stream()
                .filter(j -> j.getId().equals(id)).findFirst().orElse(null);
        if (job == null) {
            redirectAttributes.addFlashAttribute("error", "Job not found.");
            return "redirect:/jobs";
        }
        
        if (portals == null || portals.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one portal.");
            return "redirect:/jobs/" + id;
        }
        
        int successCount = 0;
        if (portals.contains("LINKEDIN") && distributionService.postToLinkedIn(job)) successCount++;
        if (portals.contains("JOBSTREET") && distributionService.postToJobStreet(job)) successCount++;
        if (portals.contains("INDEED") && distributionService.postToIndeed(job)) successCount++;

        redirectAttributes.addFlashAttribute("success", "Job successfully posted to " + successCount + " portal(s).");
        auditService.log("JOB_DISTRIBUTE", getCurrentUser(), "JobVacancy", id, "Distributed to: " + String.join(", ", portals));
        
        return "redirect:/jobs/" + id;
    }
}


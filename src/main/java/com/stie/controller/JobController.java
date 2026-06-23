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
    private com.stie.repository.CandidateApplicationRepository applicationRepository;

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

    @Autowired
    private com.stie.service.CandidateApplicationService applicationService;

    @Autowired
    private com.stie.service.CandidateService candidateService;

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
        // Sort newest on top
        all.sort((a, b) -> Long.compare(b.getId(), a.getId()));
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
        
        java.util.Map<Long, Long> totalAppsMap = new java.util.HashMap<>();
        java.util.Map<Long, java.util.Map<String, Long>> statusAppsMap = new java.util.HashMap<>();
        for (JobVacancy job : paged) {
            java.util.List<com.stie.model.CandidateApplication> apps = applicationService.getApplicationsForJob(job.getId());
            totalAppsMap.put(job.getId(), (long) apps.size());
            java.util.Map<String, Long> statusCounts = apps.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getStatus().name(), java.util.stream.Collectors.counting()));
            statusAppsMap.put(job.getId(), statusCounts);
        }

        // Global status counts for the tiles
        java.util.List<com.stie.model.Candidate> allCandidates = candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();
        long countApplied = allCandidates.stream().filter(c -> c.getStatus() == com.stie.model.Candidate.CandidateStatus.APPLIED).count();
        long countShortlisted = allCandidates.stream().filter(c -> c.getStatus() == com.stie.model.Candidate.CandidateStatus.SHORTLISTED).count();
        long countInterview = allCandidates.stream().filter(c -> c.getStatus() == com.stie.model.Candidate.CandidateStatus.INTERVIEW).count();
        long countHired = allCandidates.stream().filter(c -> c.getStatus() == com.stie.model.Candidate.CandidateStatus.HIRED).count();
        model.addAttribute("globalApplied", countApplied);
        model.addAttribute("globalShortlisted", countShortlisted);
        model.addAttribute("globalInterview", countInterview);
        model.addAttribute("globalHired", countHired);

        model.addAttribute("totalAppsMap", totalAppsMap);
        model.addAttribute("statusAppsMap", statusAppsMap);
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
        existing.setExperienceRequired(vacancy.getExperienceRequired());

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
                com.stie.model.Candidate.CandidateStatus.SHORTLISTED
        )));

        // ---------------------------------------------------------------
        // Build ranked applicant list — merge BOTH sources:
        //   1. Legacy: Candidate.jobVacancy FK (walk-ins, old imports)
        //   2. New:    CandidateApplication.jobVacancy (new tracking table)
        // ---------------------------------------------------------------
        java.util.Map<Long, com.stie.model.Candidate> candidateMap = new java.util.LinkedHashMap<>();

        // Source 1 — legacy FK
        candidateRepository.findByJobVacancy(job)
                .forEach(c -> candidateMap.put(c.getId(), c));

        // Source 2 — CandidateApplication table
        applicationRepository.findByJobVacancy(job)
                .forEach(app -> candidateMap.put(app.getCandidate().getId(), app.getCandidate()));

        java.util.List<com.stie.model.Candidate> applicants = new java.util.ArrayList<>(candidateMap.values());

        // Build a unified set of required skill keywords from:
        //   1. JobVacancy.skills (Skill entities)
        //   2. JobVacancy.category.skills (Skill entities)
        //   3. JobVacancy.description (free-text, tokenised)
        java.util.Set<String> requiredSkillKeywords = new java.util.LinkedHashSet<>();
        if (job.getSkills() != null) {
            job.getSkills().forEach(s -> requiredSkillKeywords.add(s.getName().trim().toLowerCase()));
        }
        if (job.getCategory() != null && job.getCategory().getSkills() != null) {
            job.getCategory().getSkills().forEach(s -> requiredSkillKeywords.add(s.getName().trim().toLowerCase()));
        }

        // Parse required experience (e.g. "2 years", "3+", "5") → integer
        int requiredExp = parseRequiredExperience(job.getExperienceRequired());

        java.util.List<com.stie.model.JobApplicantMatch> ranked = applicants.stream()
                .map(c -> computeMatch(c, requiredSkillKeywords, requiredExp))
                .sorted((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("rankedApplicants", ranked);
        model.addAttribute("requiredSkills", requiredSkillKeywords);
        model.addAttribute("requiredExp", requiredExp);

        // Load interviewers for the dropdown (scoped to current site)
        com.stie.model.Tenant currentSite = userService.getCurrentSite();
        model.addAttribute("interviewers", currentSite != null
                ? userService.getUsersBySite(currentSite).stream()
                    .filter(u -> u.getRole() != null && u.getRole().contains("INTERVIEWER"))
                    .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList());

        return "job-detail";
    }

    /**
     * Compute a 0-100 match score for a candidate against a job's requirements.
     * Scoring breakdown:
     *   - Skills  : up to 70 points  (each matched skill = 70 / total required skills)
     *   - Experience : up to 30 points (proportional, capped at required exp)
     */
    private com.stie.model.JobApplicantMatch computeMatch(
            com.stie.model.Candidate candidate,
            java.util.Set<String> requiredSkillKeywords,
            int requiredExp) {

        // --- Skill matching ---
        int matchedSkillCount = 0;
        int totalRequired = requiredSkillKeywords.size();

        if (candidate.getSkills() != null && !candidate.getSkills().isEmpty() && totalRequired > 0) {
            String candidateSkillsLower = candidate.getSkills().toLowerCase();
            // Also check certifications and tagged roles for bonus matches
            String bonus = "";
            if (candidate.getCertifications() != null) bonus += " " + candidate.getCertifications().toLowerCase();
            if (candidate.getTaggedRoles() != null) bonus += " " + candidate.getTaggedRoles().toLowerCase();
            String combined = candidateSkillsLower + bonus;

            for (String kw : requiredSkillKeywords) {
                if (combined.contains(kw)) matchedSkillCount++;
            }
        }

        int skillPoints = totalRequired > 0
                ? (int) Math.round((matchedSkillCount * 70.0) / totalRequired)
                : 35; // no skills defined → neutral 35/70

        // --- Experience matching ---
        int candidateExp = candidate.getExperienceYears() != null ? candidate.getExperienceYears() : 0;
        boolean experienceMet = (requiredExp == 0) || (candidateExp >= requiredExp);

        int expPoints;
        if (requiredExp == 0) {
            // No experience requirement defined → proportional based on what they have (max 30)
            expPoints = Math.min(candidateExp * 5, 30);
        } else {
            // Scale: 0 exp = 0pts, at requiredExp = 30pts, beyond = 30pts (capped)
            expPoints = (int) Math.min(Math.round((candidateExp * 30.0) / requiredExp), 30);
        }

        int totalScore = Math.min(skillPoints + expPoints, 100);

        return new com.stie.model.JobApplicantMatch(
                candidate, totalScore, matchedSkillCount, totalRequired,
                experienceMet, skillPoints, expPoints);
    }

    /**
     * Parse a human-readable experience string into an integer year value.
     * Handles: "2 years", "3+", "5-7", "At least 3", "2", null, ""
     */
    private int parseRequiredExperience(String expStr) {
        if (expStr == null || expStr.trim().isEmpty()) return 0;
        try {
            // Extract first number found
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(expStr);
            if (m.find()) return Integer.parseInt(m.group());
        } catch (Exception ignored) {}
        return 0;
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
                           @org.springframework.web.bind.annotation.RequestHeader(value = "referer", required = false) String referer,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        jobService.approveVacancy(id, note, getCurrentUser());
        redirectAttributes.addFlashAttribute("success", "Job vacancy approved successfully!");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/{id}/reject")
    public String reject(@PathVariable Long id, 
                          @RequestParam(value = "note", required = false, defaultValue = "Rejected") String note,
                          @org.springframework.web.bind.annotation.RequestHeader(value = "referer", required = false) String referer,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        jobService.rejectVacancy(id, note, getCurrentUser());
        redirectAttributes.addFlashAttribute("success", "Job vacancy rejected successfully.");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/{id}/toggle-active")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_JOBS') or hasAuthority('CREATE_JOBS')")
    public String toggleActive(@PathVariable Long id, 
                               @org.springframework.web.bind.annotation.RequestHeader(value = "referer", required = false) String referer,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        JobVacancy job = jobService.getAllVacancies().stream()
                .filter(j -> j.getId().equals(id)).findFirst().orElse(null);
        if (job != null) {
            boolean currentActive = job.isActive() != null ? job.isActive() : true;
            job.setActive(!currentActive);
            jobService.saveVacancy(job);
            redirectAttributes.addFlashAttribute("success", "Job vacancy marked as " + (!currentActive ? "Active" : "Inactive") + ".");
        } else {
            redirectAttributes.addFlashAttribute("error", "Job not found.");
        }
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
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


package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.CandidateApplication;
import com.stie.service.CandidateApplicationService;
import com.stie.service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/candidates")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private CandidateApplicationService applicationService;

    @Autowired
    private com.stie.service.AuditService auditService;

    @Autowired
    private com.stie.service.JobService jobService;

    @Autowired
    private com.stie.service.NotificationService notificationService;

    @Autowired
    private com.stie.repository.AuditLogRepository auditLogRepository;

    @Autowired
    private com.stie.service.ScorecardService scorecardService;

    @Autowired
    private com.stie.service.InterviewService interviewService;

    @Autowired
    private com.stie.service.UserService userService;

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
    }

    // =========================================================================
    // LIST
    // =========================================================================

    @GetMapping
    public String listCandidates(@RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "nationality", required = false) String nationality,
            @RequestParam(value = "minExp", required = false) Integer minExp,
            @RequestParam(value = "maxSalary", required = false) Integer maxSalary,
            @RequestParam(value = "certifications", required = false) String certifications,
            @RequestParam(value = "hasSecurityLicense", required = false) Boolean hasSecurityLicense,
            @RequestParam(value = "workPermitEligible", required = false) Boolean workPermitEligible,
            @RequestParam(value = "rankJobId", required = false) Long rankJobId,
            @RequestParam(value = "jobId", required = false) Long jobId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("pageTitle", "Candidate Database");
        model.addAttribute("jobs", jobService.getAllVacancies());
        model.addAttribute("statuses", Candidate.CandidateStatus.values());

        boolean hasFilters = (search != null && !search.isEmpty()) ||
                (nationality != null && !nationality.isEmpty()) ||
                (minExp != null && minExp > 0) ||
                (maxSalary != null && maxSalary > 0) ||
                (certifications != null && !certifications.isEmpty()) ||
                (hasSecurityLicense != null && hasSecurityLicense) ||
                (workPermitEligible != null && workPermitEligible) ||
                (rankJobId != null && rankJobId > 0) ||
                (jobId != null && jobId > 0) ||
                (status != null && !status.isEmpty());

        if (hasFilters) {
            model.addAttribute("candidates", candidateService.search(search, nationality, minExp, maxSalary,
                    certifications, hasSecurityLicense, workPermitEligible, rankJobId, jobId, status));
            model.addAttribute("isSearch", true);
        } else {
            org.springframework.data.domain.Page<Candidate> candidatePage = candidateService.getAllCandidates(
                org.springframework.data.domain.PageRequest.of(page, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
            );
            model.addAttribute("candidates", candidatePage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", candidatePage.getTotalPages());
            model.addAttribute("isSearch", false);
        }
        model.addAttribute("isCandidateDatabase", true);
        return "candidates";
    }

    @GetMapping("/applications")
    public String listApplications(@RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "nationality", required = false) String nationality,
            @RequestParam(value = "minExp", required = false) Integer minExp,
            @RequestParam(value = "maxSalary", required = false) Integer maxSalary,
            @RequestParam(value = "certifications", required = false) String certifications,
            @RequestParam(value = "hasSecurityLicense", required = false) Boolean hasSecurityLicense,
            @RequestParam(value = "workPermitEligible", required = false) Boolean workPermitEligible,
            @RequestParam(value = "rankJobId", required = false) Long rankJobId,
            @RequestParam(value = "jobId", required = false) Long jobId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("pageTitle", "Job Applications");
        model.addAttribute("jobs", jobService.getAllVacancies());
        model.addAttribute("statuses", Candidate.CandidateStatus.values());

        boolean hasFilters = (search != null && !search.isEmpty()) ||
                (nationality != null && !nationality.isEmpty()) ||
                (minExp != null && minExp > 0) ||
                (maxSalary != null && maxSalary > 0) ||
                (certifications != null && !certifications.isEmpty()) ||
                (hasSecurityLicense != null && hasSecurityLicense) ||
                (workPermitEligible != null && workPermitEligible) ||
                (rankJobId != null && rankJobId > 0) ||
                (jobId != null && jobId > 0) ||
                (status != null && !status.isEmpty());

        if (hasFilters) {
            List<Candidate> filtered = candidateService.search(search, nationality, minExp, maxSalary,
                    certifications, hasSecurityLicense, workPermitEligible, rankJobId, jobId, status)
                    .stream()
                    .filter(c -> c.getJobVacancy() != null)
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("candidates", filtered);
            model.addAttribute("isSearch", true);
        } else {
            org.springframework.data.domain.Page<Candidate> candidatePage = candidateService.getApplications(
                org.springframework.data.domain.PageRequest.of(page, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
            );
            model.addAttribute("candidates", candidatePage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", candidatePage.getTotalPages());
            model.addAttribute("isSearch", false);
        }
        model.addAttribute("isCandidateDatabase", false);
        return "candidates";
    }

    // =========================================================================
    // ADD CANDIDATE (Manual)
    // =========================================================================

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("pageTitle", "Add Candidate");
        model.addAttribute("jobs", jobService.getAllVacancies());
        model.addAttribute("sources", CandidateApplication.ApplicationSource.values());
        return "candidate-add";
    }

    @PostMapping("/add")
    public String addCandidate(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) Integer experienceYears,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) Integer expectedSalary,
            @RequestParam(required = false) String certifications,
            @RequestParam(required = false) String securityLicense,
            @RequestParam(required = false) String taggedRoles,
            @RequestParam(value = "workPermitEligible", defaultValue = "false") boolean workPermitEligible,
            @RequestParam(required = false) Long jobVacancyId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String appLocation,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        // Duplicate email check
        if (email != null && !email.trim().isEmpty()) {
            String cleanEmail = email.trim().toLowerCase();
            boolean emailExists = candidateService.getAllCandidates(PageRequest.of(0, 10000)).getContent().stream()
                    .anyMatch(c -> cleanEmail.equals(c.getEmail().trim().toLowerCase()));
            if (emailExists) {
                redirectAttributes.addFlashAttribute("error",
                        "A candidate with email '" + email + "' already exists in the database.");
                return "redirect:/candidates/add";
            }
        }

        Candidate candidate = new Candidate();
        candidate.setFullName(fullName.trim());
        candidate.setEmail(email.trim());
        candidate.setPhone(phone);
        candidate.setNationality(nationality);
        candidate.setExperienceYears(experienceYears);
        candidate.setEducations(parseEducation(education));
        candidate.setSkills(skills);
        candidate.setExpectedSalary(expectedSalary);
        candidate.setCertifications(parseCertifications(certifications));
        candidate.setSecurityLicense(securityLicense);
        candidate.setTaggedRoles(taggedRoles);
        candidate.setWorkPermitEligible(workPermitEligible);
        candidate.setTenant(userService.getCurrentTenant());

        // Link job vacancy if provided
        if (jobVacancyId != null) {
            com.stie.model.JobVacancy job = jobService.getJobById(jobVacancyId);
            candidate.setJobVacancy(job);
        }

        try {
            Candidate saved = candidateService.saveCandidate(candidate);
            auditService.log("CANDIDATE_ADDED_MANUALLY", getCurrentUser(), "Candidate", saved.getId(),
                    "Manually added candidate: " + fullName);

            // Create initial application record if a job was selected
            if (jobVacancyId != null) {
                CandidateApplication.ApplicationSource appSource = CandidateApplication.ApplicationSource.DIRECT;
                if (source != null && !source.isEmpty()) {
                    try {
                        appSource = CandidateApplication.ApplicationSource.valueOf(source.toUpperCase());
                    } catch (Exception ignored) {}
                }
                applicationService.createApplication(saved.getId(), jobVacancyId, appSource, appLocation, null, getCurrentUser());
            }

            notificationService.addNotification(
                    "New candidate added manually: " + fullName, "/candidates/" + saved.getId());
            redirectAttributes.addFlashAttribute("success",
                    "Candidate '" + fullName + "' has been successfully added to the database.");
            return "redirect:/candidates/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add candidate: " + e.getMessage());
            return "redirect:/candidates/add";
        }
    }

    // =========================================================================
    // VIEW CANDIDATE DETAIL
    // =========================================================================

    @GetMapping("/{id}")
    public String viewCandidate(@PathVariable Long id, 
            @RequestParam(required = false, defaultValue = "false") boolean fromDb,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateService.findById(id).orElse(null);

        if (candidate == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/candidates";
        }

        // Backward-compat: seed legacy application record if none exist yet
        applicationService.seedLegacyApplicationIfAbsent(candidate);

        List<com.stie.model.AuditLog> allHistory = auditService.getLogsForEntity("Candidate", id);

        List<com.stie.model.AuditLog> history = allHistory.stream()
                .filter(log -> !log.getAction().equals("CANDIDATE_NOTE"))
                .collect(java.util.stream.Collectors.toList());

        List<com.stie.model.AuditLog> notes = allHistory.stream()
                .filter(log -> log.getAction().equals("CANDIDATE_NOTE"))
                .collect(java.util.stream.Collectors.toList());

        List<CandidateApplication> applications = applicationService.getApplicationsForCandidate(id);

        model.addAttribute("candidate", candidate);
        model.addAttribute("history", history);
        model.addAttribute("notes", notes);
        model.addAttribute("scorecards", scorecardService.getScorecardsByCandidate(id));
        model.addAttribute("applications", applications);
        model.addAttribute("jobs", jobService.getAllVacanciesAcrossTenants());
        model.addAttribute("appSources", CandidateApplication.ApplicationSource.values());
        model.addAttribute("appStatuses", CandidateApplication.AppStatus.values());
        model.addAttribute("fromDb", fromDb);
        model.addAttribute("pageTitle", "Candidate Profile");
        return "candidate-detail";
    }

    // =========================================================================
    // ADD APPLICATION (for existing candidate)
    // =========================================================================

    @PostMapping("/{id}/applications")
    public String addApplication(@PathVariable Long id,
            @RequestParam(required = false) Long jobVacancyId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String notes,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        CandidateApplication.ApplicationSource appSource = CandidateApplication.ApplicationSource.DIRECT;
        if (source != null && !source.isEmpty()) {
            try {
                appSource = CandidateApplication.ApplicationSource.valueOf(source.toUpperCase());
            } catch (Exception ignored) {}
        }

        java.util.Optional<CandidateApplication> result =
                applicationService.createApplication(id, jobVacancyId, appSource, location, notes, getCurrentUser());

        if (result.isPresent()) {
            redirectAttributes.addFlashAttribute("success", "Application record added successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Could not add application. The candidate may have already applied to this job.");
        }
        return "redirect:/candidates/" + id;
    }

    // =========================================================================
    // UPDATE APPLICATION STATUS
    // =========================================================================

    @PostMapping("/{id}/applications/{appId}/status")
    public String updateApplicationStatus(
            @PathVariable Long id,
            @PathVariable Long appId,
            @RequestParam CandidateApplication.AppStatus status,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        try {
            boolean updated = applicationService.updateApplicationStatus(appId, status, getCurrentUser());
            if (updated) {
                redirectAttributes.addFlashAttribute("success", "Application status updated to " + status + ".");
            } else {
                redirectAttributes.addFlashAttribute("error", "Application not found.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/candidates/" + id;
    }

    // =========================================================================
    // SHARE WITH HIRING MANAGER
    // =========================================================================

    @PostMapping("/{id}/share")
    public String shareWithHM(@PathVariable Long id,
            @RequestParam("hmEmail") String hmEmail,
            @RequestParam("hmName") String hmName,
            javax.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        Candidate candidate = candidateService.findById(id).orElse(null);

        if (candidate == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/candidates";
        }

        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
        String candidateUrl = baseUrl + "/candidates/" + candidate.getId();

        notificationService.shareCandidateWithHM(hmEmail, hmName, candidate.getFullName(), candidateUrl);
        candidateService.updateSharedWithHM(id, true);

        notificationService.addNotification("Candidate " + candidate.getFullName() + " shared with Hiring Manager "
                + hmName + " (" + hmEmail + ") for review.", "/candidates/" + candidate.getId());

        auditService.log("CANDIDATE_SHARED_WITH_HM", getCurrentUser(), "Candidate", id,
                "Shared candidate: " + candidate.getFullName() + " with Hiring Manager: " + hmName + " (" + hmEmail + ")");

        redirectAttributes.addFlashAttribute("success",
                "Candidate profile shared successfully with HM " + hmName + "!");
        return "redirect:/candidates/" + id;
    }

    // =========================================================================
    // STATUS UPDATE (top-level candidate status, legacy)
    // =========================================================================

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam Candidate.CandidateStatus status,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateService.findById(id).orElse(null);
        if (candidate != null) {
            try {
                candidateService.updateStatus(id, status, getCurrentUser());
                // Notify candidate by email
                if (status != Candidate.CandidateStatus.SHORTLISTED) {
                    try {
                        CandidateApplication.AppStatus appStatus = CandidateApplication.AppStatus.valueOf(status.name());
                        String jobTitle = candidate.getJobVacancy() != null ? candidate.getJobVacancy().getTitle() : null;
                        notificationService.sendApplicationStatusUpdateEmail(
                                candidate.getEmail(), candidate.getFullName(), jobTitle, appStatus);
                    } catch (Exception ignored) {}
                }
                redirectAttributes.addFlashAttribute("success", "Candidate status updated to " + status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
        }
        return "redirect:/candidates/" + id;
    }

    // =========================================================================
    // NOTES
    // =========================================================================

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id, @RequestParam String note,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateService.findById(id).orElse(null);
        if (candidate != null && note != null && !note.trim().isEmpty()) {
            auditService.log("CANDIDATE_NOTE", getCurrentUser(), "Candidate", id, note.trim());
            redirectAttributes.addFlashAttribute("success", "Recruitment note added successfully.");
        }
        return "redirect:/candidates/" + id;
    }

    // =========================================================================
    // DOCUMENT UPLOAD
    // =========================================================================

    @Autowired
    private com.stie.service.ParserService parserService;


    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @PostMapping("/{id}/upload")
    public String uploadDocument(@PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        Candidate existing = candidateService.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/candidates";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/candidates/" + id;
        }

        if ("Resume".equalsIgnoreCase(type)) {
            Candidate parsed = parserService.parseResume(file);

            if (parsed.getEmail() != null && !parsed.getEmail().trim().isEmpty()) {
                String parsedEmail = parsed.getEmail().trim().toLowerCase();
                boolean emailExists = candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 10000)).getContent()
                        .stream()
                        .anyMatch(c -> !c.getId().equals(id)
                                && parsedEmail.equals(c.getEmail().trim().toLowerCase()));
                if (emailExists) {
                    redirectAttributes.addFlashAttribute("error", "Failed to update profile: The email '"
                            + parsed.getEmail() + "' is already registered to another candidate.");
                    return "redirect:/candidates/" + id;
                }
                existing.setEmail(parsed.getEmail());
            }

            if (parsed.getFullName() != null && !parsed.getFullName().isEmpty())
                existing.setFullName(parsed.getFullName());
            if (parsed.getPhone() != null && !parsed.getPhone().isEmpty())
                existing.setPhone(parsed.getPhone());
            if (parsed.getNationality() != null && !parsed.getNationality().isEmpty())
                existing.setNationality(parsed.getNationality());
            if (parsed.getExperienceYears() != null && parsed.getExperienceYears() > 0)
                existing.setExperienceYears(parsed.getExperienceYears());
            if (parsed.getEducations() != null && !parsed.getEducations().isEmpty())
                existing.setEducations(parsed.getEducations());
            if (parsed.getSkills() != null && !parsed.getSkills().isEmpty())
                existing.setSkills(parsed.getSkills());
            if (parsed.getCertifications() != null && !parsed.getCertifications().isEmpty())
                existing.setCertifications(parsed.getCertifications());

            try {
                String uploadDir = com.stie.config.AppConstants.FilePaths.UPLOAD_DIR;
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) java.nio.file.Files.createDirectories(uploadPath);
                String fileName = java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                java.nio.file.Files.copy(file.getInputStream(), uploadPath.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                existing.setResumePath(fileName);
                
                candidateService.saveCandidate(existing);
                redirectAttributes.addFlashAttribute("success", "Resume parsed and profile updated.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Failed to save profile changes: " + e.getMessage());
                return "redirect:/candidates/" + id;
            }
        } else {
            try {
                String uploadDir = com.stie.config.AppConstants.FilePaths.UPLOAD_DIR;
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) java.nio.file.Files.createDirectories(uploadPath);
                
                String fileName = java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                java.nio.file.Files.copy(file.getInputStream(), uploadPath.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                if ("CERT".equalsIgnoreCase(type)) {
                    existing.setAcademicCertPath(fileName);
                } else if ("OTHER".equalsIgnoreCase(type)) {
                    existing.setOtherDocPath(fileName);
                } else if ("PASSPORT".equalsIgnoreCase(type)) {
                    // Not requested but could be mapped if needed, keeping it minimal
                }
                
                candidateService.saveCandidate(existing);
                redirectAttributes.addFlashAttribute("success", "Document uploaded successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload document: " + e.getMessage());
                return "redirect:/candidates/" + id;
            }
        }

        auditService.log("DOCUMENT_UPLOAD", getCurrentUser(), "Candidate", id,
                "Type: " + type + ", File: " + file.getOriginalFilename());
        return "redirect:/candidates/" + id;
    }

    // =========================================================================
    // EDIT CANDIDATE DETAILS
    // =========================================================================

    @PostMapping("/{id}/edit")
    public String editCandidate(@PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String nationality,
            @RequestParam Integer experienceYears,
            @RequestParam String education,
            @RequestParam String skills,
            @RequestParam(value = "expectedSalary", required = false) Integer expectedSalary,
            @RequestParam(value = "certifications", required = false) String certifications,
            @RequestParam(value = "securityLicense", required = false) String securityLicense,
            @RequestParam(value = "taggedRoles", required = false) String taggedRoles,
            @RequestParam(value = "projects", required = false) String projects,
            @RequestParam(value = "projectUrl", required = false) String projectUrl,
            @RequestParam(value = "workPermitEligible", defaultValue = "false") boolean workPermitEligible,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        Candidate existing = candidateService.findById(id).orElse(null);

        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/candidates";
        }

        if (email != null && !email.trim().isEmpty()) {
            String cleanEmail = email.trim().toLowerCase();
            boolean emailExists = candidateService.getAllCandidates(PageRequest.of(0, 10000)).getContent().stream()
                    .anyMatch(c -> !c.getId().equals(id) && cleanEmail.equals(c.getEmail().trim().toLowerCase()));
            if (emailExists) {
                redirectAttributes.addFlashAttribute("error", "Failed to update profile: The email '" + email
                        + "' is already registered to another candidate.");
                return "redirect:/candidates/" + id;
            }
        }

        existing.setFullName(fullName);
        existing.setEmail(email);
        existing.setPhone(phone);
        existing.setNationality(nationality);
        existing.setExperienceYears(experienceYears);
        existing.setEducations(parseEducation(education));
        existing.setSkills(skills);
        existing.setExpectedSalary(expectedSalary);
        existing.setCertifications(parseCertifications(certifications));
        existing.setSecurityLicense(securityLicense);
        existing.setTaggedRoles(taggedRoles);
        existing.setProjects(projects);
        existing.setProjectUrl(projectUrl);
        existing.setWorkPermitEligible(workPermitEligible);

        try {
            candidateService.saveCandidate(existing);
            auditService.log("CANDIDATE_UPDATE", getCurrentUser(), "Candidate", id, "Updated profile fields manually.");
            redirectAttributes.addFlashAttribute("success", "Candidate details updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update candidate details: " + e.getMessage());
        }
        return "redirect:/candidates/" + id;
    }

    // =========================================================================
    // EXPORT CSV
    // =========================================================================

    @GetMapping("/export")
    public void exportCandidates(javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=candidates_export.csv");

        java.io.PrintWriter writer = response.getWriter();
        writer.println("ID,Name,Email,Phone,Nationality,Experience,Skills,Education,ExpectedSalary,Status,AppliedAt,TotalApplications");

        for (Candidate c : candidateService.getAllCandidates(PageRequest.of(0, 10000)).getContent()) {
            long appCount = applicationService.countApplicationsForCandidate(c.getId());
            writer.println(String.format("%d,\"%s\",%s,%s,%s,%d,\"%s\",\"%s\",%s,%s,%s,%d",
                    c.getId(),
                    c.getFullName(),
                    c.getEmail() != null ? c.getEmail() : "",
                    c.getPhone() != null ? c.getPhone() : "",
                    c.getNationality() != null ? c.getNationality() : "",
                    c.getExperienceYears() != null ? c.getExperienceYears() : 0,
                    c.getSkills() != null ? c.getSkills() : "",
                    c.getEducations() != null ? c.getEducations().size() + " records" : "",
                    c.getExpectedSalary() != null ? c.getExpectedSalary() : "",
                    c.getStatus(),
                    c.getAppliedAt() != null ? c.getAppliedAt() : "",
                    appCount));
        }
    }

    @GetMapping("/debug/candidates")
    @org.springframework.web.bind.annotation.ResponseBody
    public String debugCandidates() {
        StringBuilder sb = new StringBuilder();
        for (Candidate c : candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent()) {
            sb.append("ID: ").append(c.getId())
              .append(", Email: ").append(c.getEmail())
              .append(", Name: ").append(c.getFullName())
              .append(", JobID: ").append(c.getJobVacancy() != null ? c.getJobVacancy().getId() : "null")
              .append(", Apps: ").append(applicationService.getApplicationsForCandidate(c.getId()).size())
              .append("<br>");
        }
        return sb.toString();
    }

    // =========================================================================
    // KANBAN
    // =========================================================================

    @GetMapping("/kanban")
    public String showKanban(Model model) {
        model.addAttribute("pageTitle", "Pipeline Stage Kanban Board");
        
        List<Candidate> candidates = candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 1000, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt"))).getContent();
        model.addAttribute("candidates", candidates);
        model.addAttribute("jobs", jobService.getAllVacancies());
        model.addAttribute("statuses", Candidate.CandidateStatus.values());
        
        java.util.Set<Long> completedInterviewCandidateIds = candidates.stream()
            .filter(c -> c.getStatus() == Candidate.CandidateStatus.INTERVIEW)
            .filter(c -> {
                java.util.List<com.stie.model.InterviewScorecard> scorecards = scorecardService.getScorecardsByCandidate(c.getId());
                if (!scorecards.isEmpty()) return true;
                java.util.List<com.stie.model.Interview> interviews = interviewService.getInterviewsByCandidate(c.getId());
                return interviews.stream().anyMatch(i -> i.getStatus() == com.stie.model.Interview.InterviewStatus.COMPLETED || (i.getFeedback() != null && !i.getFeedback().trim().isEmpty()));
            })
            .map(Candidate::getId)
            .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("completedInterviewCandidateIds", completedInterviewCandidateIds);

        com.stie.model.Tenant currentSite = userService.getCurrentSite();
        model.addAttribute("interviewers", currentSite != null
                ? userService.getUsersBySite(currentSite).stream()
                    .filter(u -> u.getRole() != null && u.getRole().contains("INTERVIEWER"))
                    .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList());
                
        return "candidates-kanban";
    }

    @PostMapping("/{id}/move")
    public String moveCandidate(@PathVariable Long id, 
            @RequestParam("status") String statusStr,
            @RequestParam(value="interviewerId", required=false) Long interviewerId,
            @RequestParam(value="rejectionRemarks", required=false) String rejectionRemarks,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Candidate.CandidateStatus status = Candidate.CandidateStatus.valueOf(statusStr.toUpperCase());
            candidateService.updateStatus(id, status, getCurrentUser());

            // Save rejection remarks when rejecting
            if (status == Candidate.CandidateStatus.REJECTED && rejectionRemarks != null && !rejectionRemarks.trim().isEmpty()) {
                Candidate c = candidateService.findById(id).orElse(null);
                if (c != null) {
                    c.setRejectionRemarks(rejectionRemarks);
                    candidateService.saveCandidate(c);
                }
            }
            
            if (status == Candidate.CandidateStatus.INTERVIEW && interviewerId != null) {
                com.stie.model.User interviewer = userService.findById(interviewerId);
                if (interviewer != null) {
                    candidateService.assignInterviewer(id, interviewer);
                    auditService.log("CANDIDATE_FORWARDED", getCurrentUser(), "Candidate", id, "Forwarded to Interviewer: " + (interviewer.getDisplayName() != null ? interviewer.getDisplayName() : interviewer.getUsername()));
                    notificationService.addNotification("You have been assigned to interview candidate " + candidateService.findById(id).get().getFullName(), appBaseUrl + "/candidates/" + id, interviewer.getUsername());
                }
            }
            // Notify candidate by email
            Candidate candidate = candidateService.findById(id).orElse(null);
            if (candidate != null && status != Candidate.CandidateStatus.SHORTLISTED) {
                try {
                    CandidateApplication.AppStatus appStatus = CandidateApplication.AppStatus.valueOf(status.name());
                    String jobTitle = candidate.getJobVacancy() != null ? candidate.getJobVacancy().getTitle() : null;
                    notificationService.sendApplicationStatusUpdateEmail(
                            candidate.getEmail(), candidate.getFullName(), jobTitle, appStatus);
                } catch (Exception ignored) {}
            }
            redirectAttributes.addFlashAttribute("success", "Candidate shifted to " + status + " stage successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to shift stage: " + e.getMessage());
        }
        return "redirect:/candidates/kanban";
    }

    @GetMapping("/hire/{id}")
    public String showHireForm(@PathVariable Long id, Model model) {
        Candidate candidate = candidateService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid candidate id: " + id));
        model.addAttribute("candidate", candidate);
        model.addAttribute("pageTitle", "Finalize Hire Details");
        return "hire-form";
    }

    @PostMapping("/hire")
    public String processHire(
            @RequestParam Long candidateId,
            @RequestParam Double finalSalary,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate joiningDate,
            @RequestParam(required = false) String hireNotes,
            @RequestParam(value = "signedOfferFile", required = false) MultipartFile signedOfferFile,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        Candidate candidate = candidateService.findById(candidateId).orElseThrow(() -> new IllegalArgumentException("Invalid candidate id: " + candidateId));
        
        candidate.setFinalSalary(finalSalary);
        candidate.setJoiningDate(joiningDate);
        candidate.setHireNotes(hireNotes);
        
        if (signedOfferFile != null && !signedOfferFile.isEmpty()) {
            try {
                String uploadDir = com.stie.config.AppConstants.FilePaths.SIGNED_OFFERS_SUBDIR;
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                String filename = java.util.UUID.randomUUID().toString() + "_" + org.springframework.util.StringUtils.cleanPath(signedOfferFile.getOriginalFilename());
                java.nio.file.Path filePath = uploadPath.resolve(filename);
            try (java.io.InputStream is = signedOfferFile.getInputStream()) {
                java.nio.file.Files.copy(is, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
                candidate.setSignedOfferLetterPath("offers/signed/" + filename);
            } catch (java.io.IOException e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload signed offer letter.");
                return "redirect:/candidates/hire/" + candidateId;
            }
        }

        candidate.setStatus(Candidate.CandidateStatus.HIRED);
        
        candidateService.saveCandidate(candidate);

        // Optionally send a notification here
        
        auditService.log("CANDIDATE_HIRED", getCurrentUser(), "Candidate", candidateId, 
            "Candidate hired with final salary $" + finalSalary + ", joining " + joiningDate);

        redirectAttributes.addFlashAttribute("success", "Candidate hired successfully with final details.");
        return "redirect:/candidates/kanban";
    }

    @GetMapping("/view-offer/{id}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> viewOfferLetter(@PathVariable Long id) {
        Candidate candidate = candidateService.findById(id).orElse(null);
        if (candidate == null || candidate.getOfferLetterPath() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        try {
            java.nio.file.Path file = java.nio.file.Paths.get(com.stie.config.AppConstants.FilePaths.OFFERS_SUBDIR).resolve(candidate.getOfferLetterPath());
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return org.springframework.http.ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                        .body(resource);
            } else {
                return org.springframework.http.ResponseEntity.notFound().build();
            }
        } catch (java.net.MalformedURLException e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/update-salary/{id}")
    public String updateSalary(@PathVariable Long id, 
            @RequestParam(required = false) String basic, 
            @RequestParam(required = false) String transport, 
            @RequestParam(required = false) String mobile, 
            @RequestParam(required = false) String other,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateService.findById(id).orElse(null);
        if (candidate != null) {
            com.stie.model.Salary salary = null;
            if (candidate.getSalaries() != null && !candidate.getSalaries().isEmpty()) {
                salary = candidate.getSalaries().get(candidate.getSalaries().size() - 1);
            } else {
                salary = new com.stie.model.Salary();
                salary.setCandidate(candidate);
                if (candidate.getSalaries() == null) {
                    candidate.setSalaries(new java.util.ArrayList<>());
                }
                candidate.getSalaries().add(salary);
            }
            salary.setBasic(basic);
            salary.setTransport(transport);
            salary.setMobile(mobile);
            salary.setOther(other);
            
            double basicVal = basic != null && !basic.isEmpty() ? Double.parseDouble(basic) : 0;
            double transVal = transport != null && !transport.isEmpty() ? Double.parseDouble(transport) : 0;
            double mobVal = mobile != null && !mobile.isEmpty() ? Double.parseDouble(mobile) : 0;
            double otherVal = other != null && !other.isEmpty() ? Double.parseDouble(other) : 0;
            candidate.setFinalSalary(basicVal + transVal + mobVal + otherVal);
            
            candidateService.saveCandidate(candidate);
            redirectAttributes.addFlashAttribute("success", "Salary details updated successfully for " + candidate.getFullName());
        }
        return "redirect:/candidates";
    }

    private java.util.List<com.stie.model.CandidateEducation> parseEducation(String raw) {
        java.util.List<com.stie.model.CandidateEducation> list = new java.util.ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return list;
        for (String part : raw.split(",")) {
            if (part.trim().isEmpty()) continue;
            String[] chunks = part.split("\\|");
            com.stie.model.CandidateEducation ce = new com.stie.model.CandidateEducation();
            if (chunks.length > 0) ce.setInstitution(chunks[0].trim());
            if (chunks.length > 1) ce.setDegree(chunks[1].trim());
            if (chunks.length > 2) ce.setStartYear(chunks[2].trim());
            if (chunks.length > 3) ce.setEndYear(chunks[3].trim());
            if (chunks.length > 4) {
                String cStr = chunks[4].trim();
                if (cStr.startsWith("CGPA:")) {
                    try { ce.setCgpa(Double.parseDouble(cStr.replace("CGPA:", "").trim())); } catch (Exception ex){}
                }
            }
            list.add(ce);
        }
        return list;
    }

    private java.util.List<String> parseCertifications(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new java.util.ArrayList<>();
        return java.util.Arrays.asList(raw.split("\\s*,\\s*"));
    }

    @PostMapping("/{id}/delete")
    public String deleteCandidate(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes, org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"))) {
            try {
                candidateService.deleteCandidate(id);
                redirectAttributes.addFlashAttribute("success", "Candidate deleted successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error deleting candidate: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Unauthorized to delete candidates.");
        }
        return "redirect:/candidates";
    }
}

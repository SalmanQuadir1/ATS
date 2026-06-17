package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/candidates")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;

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

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
    }

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
            Page<Candidate> candidatePage = candidateService.getAllCandidates(PageRequest.of(page, 10));
            model.addAttribute("candidates", candidatePage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", candidatePage.getTotalPages());
            model.addAttribute("isSearch", false);
        }
        return "candidates";
    }

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

        // Share via Notification Service
        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
        String candidateUrl = baseUrl + "/candidates/" + candidate.getId();

        notificationService.shareCandidateWithHM(hmEmail, hmName, candidate.getFullName(), candidateUrl);
        candidateService.updateSharedWithHM(id, true);

        notificationService.addNotification("Candidate " + candidate.getFullName() + " shared with Hiring Manager "
                + hmName + " (" + hmEmail + ") for review.", "/candidates/" + candidate.getId());

        auditService.log("CANDIDATE_SHARED_WITH_HM", getCurrentUser(), "Candidate", id,
                "Shared candidate: " + candidate.getFullName() + " with Hiring Manager: " + hmName + " (" + hmEmail
                        + ")");

        redirectAttributes.addFlashAttribute("success",
                "Candidate profile shared successfully with HM " + hmName + "!");
        return "redirect:/candidates/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam Candidate.CandidateStatus status,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        candidateService.updateStatus(id, status);
        auditService.log("CANDIDATE_STATUS_CHANGE", getCurrentUser(), "Candidate", id, "New Status: " + status);
        redirectAttributes.addFlashAttribute("success", "Candidate status updated to " + status);
        return "redirect:/candidates/" + id;
    }

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id, @RequestParam String note,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
                .filter(c -> c.getId().equals(id)).findFirst().orElse(null);
        if (candidate != null && note != null && !note.trim().isEmpty()) {
            auditService.log("CANDIDATE_NOTE", getCurrentUser(), "Candidate", id, note.trim());
            redirectAttributes.addFlashAttribute("success", "Recruitment note added successfully.");
        }
        return "redirect:/candidates/" + id;
    }

    @GetMapping("/{id}")
    public String viewCandidate(@PathVariable Long id, Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
                .filter(c -> c.getId().equals(id)).findFirst().orElse(null);

        if (candidate == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/candidates";
        }

        java.util.List<com.stie.model.AuditLog> allHistory = auditService.getLogsForEntity("Candidate", id);

        java.util.List<com.stie.model.AuditLog> history = allHistory.stream()
                .filter(log -> !log.getAction().equals("CANDIDATE_NOTE"))
                .collect(java.util.stream.Collectors.toList());

        java.util.List<com.stie.model.AuditLog> notes = allHistory.stream()
                .filter(log -> log.getAction().equals("CANDIDATE_NOTE"))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("candidate", candidate);
        model.addAttribute("history", history);
        model.addAttribute("notes", notes);
        model.addAttribute("scorecards", scorecardService.getScorecardsByCandidate(id));
        return "candidate-detail";
    }

    @Autowired
    private com.stie.service.ParserService parserService;

    @PostMapping("/{id}/upload")
    public String uploadDocument(@PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if ("Resume".equalsIgnoreCase(type) && !file.isEmpty()) {
            Candidate existing = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
                    .filter(c -> c.getId().equals(id)).findFirst().orElse(null);

            if (existing != null) {
                Candidate parsed = parserService.parseResume(file);

                // Pre-check for duplicate email prior to saving
                if (parsed.getEmail() != null && !parsed.getEmail().trim().isEmpty()) {
                    String parsedEmail = parsed.getEmail().trim().toLowerCase();
                    boolean emailExists = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent()
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

                // Overwrite candidate fields with successfully parsed details from the newly
                // uploaded PDF
                if (parsed.getFullName() != null && !parsed.getFullName().isEmpty())
                    existing.setFullName(parsed.getFullName());
                if (parsed.getPhone() != null && !parsed.getPhone().isEmpty())
                    existing.setPhone(parsed.getPhone());
                if (parsed.getNationality() != null && !parsed.getNationality().isEmpty())
                    existing.setNationality(parsed.getNationality());
                if (parsed.getExperienceYears() != null && parsed.getExperienceYears() > 0)
                    existing.setExperienceYears(parsed.getExperienceYears());
                if (parsed.getEducation() != null && !parsed.getEducation().isEmpty())
                    existing.setEducation(parsed.getEducation());
                if (parsed.getSkills() != null && !parsed.getSkills().isEmpty())
                    existing.setSkills(parsed.getSkills());

                try {
                    candidateService.saveCandidate(existing);
                    redirectAttributes.addFlashAttribute("success", "Resume parsed and profile updated.");
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Failed to save profile changes: " + e.getMessage());
                    return "redirect:/candidates/" + id;
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Candidate not found.");
                return "redirect:/candidates";
            }
        } else if (!file.isEmpty()) {
            redirectAttributes.addFlashAttribute("success", "Document uploaded successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/candidates/" + id;
        }

        // In a real app, save file to storage and update candidate.documents list
        System.out.println("Uploaded " + type + " for candidate " + id + ": " + file.getOriginalFilename());
        auditService.log("DOCUMENT_UPLOAD", getCurrentUser(), "Candidate", id,
                "Type: " + type + ", File: " + file.getOriginalFilename());
        return "redirect:/candidates/" + id;
    }

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

        Candidate existing = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
                .filter(c -> c.getId().equals(id)).findFirst().orElse(null);

        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/candidates";
        }

        if (email != null && !email.trim().isEmpty()) {
            String cleanEmail = email.trim().toLowerCase();
            boolean emailExists = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
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
        existing.setEducation(education);
        existing.setSkills(skills);
        existing.setExpectedSalary(expectedSalary);
        existing.setCertifications(certifications);
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

    @GetMapping("/export")
    public void exportCandidates(javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; file=candidates_export.csv");

        java.io.PrintWriter writer = response.getWriter();
        writer.println("ID,Name,Email,Nationality,Experience,Status");

        for (Candidate c : candidateService.getAllCandidates(PageRequest.of(0, 10000)).getContent()) {
            writer.println(String.format("%d,%s,%s,%s,%d,%s",
                    c.getId(), c.getFullName(), c.getEmail(), c.getNationality(), c.getExperienceYears(),
                    c.getStatus()));
        }
    }

    @GetMapping("/kanban")
    public String showKanban(Model model) {
        model.addAttribute("pageTitle", "Pipeline Stage Kanban Board");
        model.addAttribute("candidates", candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent());
        model.addAttribute("jobs", jobService.getAllVacancies());
        model.addAttribute("statuses", Candidate.CandidateStatus.values());
        return "candidates-kanban";
    }

    @PostMapping("/{id}/move")
    public String moveCandidate(@PathVariable Long id, @RequestParam("status") String statusStr,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Candidate.CandidateStatus status = Candidate.CandidateStatus.valueOf(statusStr.toUpperCase());
            candidateService.updateStatus(id, status);
            auditService.log("CANDIDATE_STAGE_SHIFT", getCurrentUser(), "Candidate", id,
                    "Moved to pipeline stage: " + status);
            redirectAttributes.addFlashAttribute("success", "Candidate shifted to " + status + " stage successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to shift stage: " + e.getMessage());
        }
        return "redirect:/candidates/kanban";
    }
}


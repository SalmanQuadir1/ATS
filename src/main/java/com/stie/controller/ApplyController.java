package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.JobVacancy;
import com.stie.service.BrandingService;
import com.stie.service.CandidateService;
import com.stie.service.JobService;
import com.stie.service.NotificationService;
import com.stie.service.ParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ApplyController {

    @Autowired
    private JobService jobService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private com.stie.service.CandidateApplicationService applicationService;

    @Autowired
    private ParserService parserService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private com.stie.service.TenantService tenantService;

    @GetMapping("/{tenantName}/apply/{id}")
    public String showApplyForm(@PathVariable("tenantName") String tenantName, @PathVariable("id") Long id, Model model,
            RedirectAttributes ra) {
        System.out.println("DEBUG: showApplyForm called for job ID: " + id);
        com.stie.model.Tenant tenant = tenantService.getSiteBySubdomain(tenantName);
        if (tenant == null) {
            ra.addFlashAttribute("error", "Tenant not found: " + tenantName);
            return "redirect:/login";
        }
        JobVacancy job = jobService.getJobById(id);
        System.out.println("DEBUG: Job found: " + (job != null ? job.getTitle() : "null"));
        if (job != null) {
            System.out.println("DEBUG: Job Tenant: " + (job.getTenant() != null ? job.getTenant().getId() : "null"));
            System.out.println("DEBUG: Request Tenant: " + tenant.getId());
            System.out.println("DEBUG: Job Expired: " + job.isExpired() + " | ExpiryDate: " + job.getExpiryDate());
            System.out.println("DEBUG: Job Active: " + job.isActive());
        }
        if (job == null || job.getTenant() == null || !job.getTenant().getId().equals(tenant.getId())) {
            ra.addFlashAttribute("error", "Job vacancy not found with ID: " + id);
            return "redirect:/" + tenantName + "/landing";
        }
        if (job.isExpired() || (job.isActive() != null && !job.isActive())) {
            ra.addFlashAttribute("error", "This job vacancy is no longer active or has expired.");
            return "redirect:/" + tenantName + "/landing";
        }
        model.addAttribute("tenant", tenant);
        model.addAttribute("branding", brandingService.getBranding(tenant));
        model.addAttribute("job", job);
        model.addAttribute("candidate", new Candidate());
        return "apply";
    }

    @PostMapping("/{tenantName}/apply/{id}/submit")
    public String submitApplication(@PathVariable("tenantName") String tenantName, @PathVariable("id") Long id,
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "academicCert", required = false) MultipartFile academicCert,
            @RequestParam(value = "otherDoc", required = false) MultipartFile otherDoc,
            @RequestParam(value = "educationRaw", required = false) String educationRaw,
            @RequestParam(value = "certificationsRaw", required = false) String certificationsRaw,
            Candidate candidate,
            Model model) {
        System.out.println("DEBUG: submitApplication called for job ID: " + id);
        System.out.println("DEBUG: Candidate name: " + candidate.getFullName() + ", email: " + candidate.getEmail());

        com.stie.model.Tenant tenant = tenantService.getSiteBySubdomain(tenantName);
        if (tenant == null)
            return "redirect:/login";

        JobVacancy job = jobService.getJobById(id);
        System.out.println("DEBUG: Job found: " + (job != null ? job.getTitle() : "null"));

        if (job == null || job.getTenant() == null || !job.getTenant().getId().equals(tenant.getId())) {
            model.addAttribute("error", "Job vacancy not found with ID: " + id);
            model.addAttribute("tenant", tenant);
            model.addAttribute("branding", brandingService.getBranding(tenant));
            return "apply";
        }
        if (job.isExpired() || (job.isActive() != null && !job.isActive())) {
            model.addAttribute("error", "This job vacancy is no longer accepting applications.");
            model.addAttribute("tenant", tenant);
            model.addAttribute("branding", brandingService.getBranding(tenant));
            return "apply";
        }

        if (resume == null || resume.isEmpty()) {
            model.addAttribute("error", "Resume upload is mandatory.");
            model.addAttribute("tenant", tenant);
            model.addAttribute("branding", brandingService.getBranding(tenant));
            model.addAttribute("job", job);
            return "apply";
        }

        model.addAttribute("tenant", tenant);
        model.addAttribute("branding", brandingService.getBranding(tenant));
        model.addAttribute("job", job);
        candidate.setJobVacancy(job);
        candidate.setTenant(tenant);

        String uploadDir = com.stie.config.AppConstants.FilePaths.UPLOAD_DIR;
        Path uploadPath = Paths.get(uploadDir);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (resume != null && !resume.isEmpty()) {
            try {
                String resumeFileName = UUID.randomUUID().toString() + "_" + resume.getOriginalFilename();
                Path filePath = uploadPath.resolve(resumeFileName);
                try (java.io.InputStream is = resume.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                candidate.setResumePath(resumeFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (photo != null && !photo.isEmpty()) {
            try {
                String photoFileName = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
                Path filePath = uploadPath.resolve(photoFileName);
                try (java.io.InputStream is = photo.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                candidate.setPhotoPath(photoFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (academicCert != null && !academicCert.isEmpty()) {
            try {
                String certFileName = UUID.randomUUID().toString() + "_" + academicCert.getOriginalFilename();
                Path filePath = uploadPath.resolve(certFileName);
                try (java.io.InputStream is = academicCert.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                candidate.setAcademicCertPath(certFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (otherDoc != null && !otherDoc.isEmpty()) {
            try {
                String docFileName = UUID.randomUUID().toString() + "_" + otherDoc.getOriginalFilename();
                Path filePath = uploadPath.resolve(docFileName);
                try (java.io.InputStream is = otherDoc.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                candidate.setOtherDocPath(docFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            // Fix Spring MVC data binding bug: The @PathVariable("id") representing the Job ID
            // gets automatically bound to candidate.setId() by Spring. We MUST clear it
            // so Hibernate treats this as a brand new Candidate insertion instead of updating
            // an existing Candidate with that ID.
            candidate.setId(null);

            // New candidate entirely
            if (educationRaw != null && !educationRaw.trim().isEmpty()) {
                java.util.List<com.stie.model.CandidateEducation> edList = new java.util.ArrayList<>();
                String[] edParts = educationRaw.split(",");
                for (String edPart : edParts) {
                    if (edPart.trim().isEmpty()) continue;
                    String[] chunks = edPart.split("\\|");
                    com.stie.model.CandidateEducation ce = new com.stie.model.CandidateEducation();
                    if (chunks.length > 0) ce.setInstitution(chunks[0].trim());
                    if (chunks.length > 1) ce.setDegree(chunks[1].trim());
                    if (chunks.length > 2) ce.setStartYear(chunks[2].trim());
                    if (chunks.length > 3) ce.setEndYear(chunks[3].trim());
                    if (chunks.length > 4) {
                        String cgpaStr = chunks[4].trim();
                        if (cgpaStr.startsWith("CGPA:")) {
                            try {
                                ce.setCgpa(Double.parseDouble(cgpaStr.replace("CGPA:", "").trim()));
                            } catch (Exception ex) {}
                        }
                    }
                    edList.add(ce);
                }
                candidate.setEducations(edList);
            }
            if (certificationsRaw != null && !certificationsRaw.trim().isEmpty()) {
                candidate.setCertifications(java.util.Arrays.asList(certificationsRaw.split("\\s*,\\s*")));
            }

            candidateService.saveCandidate(candidate);

            // Still create the CandidateApplication record for history/tracking
            applicationService.createApplication(candidate.getId(), job.getId(),
                    com.stie.model.CandidateApplication.ApplicationSource.ONLINE, null, null, "Public Portal");

            notificationService.addNotification(
                    "New application for " + job.getTitle() + ": " + candidate.getFullName(),
                    "/candidates/" + candidate.getId());
            notificationService.sendAcknowledgment(candidate.getEmail(), candidate.getFullName());
            model.addAttribute("message", "Application for " + job.getTitle()
                    + " submitted successfully! Our HR team will contact you soon.");
            return "success";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to submit application: " + e.getMessage());
            model.addAttribute("tenant", tenant);
            model.addAttribute("branding", brandingService.getBranding(tenant));
            model.addAttribute("job", job);
            return "apply";
        }
    }
}

package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.service.CandidateService;
import com.stie.service.NotificationService;
import com.stie.service.ParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class WalkInController {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private ParserService parserService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.stie.service.BrandingService brandingService;

    @Autowired
    private com.stie.service.JobService jobService;

    @Autowired
    private com.stie.service.TenantService tenantService;

    @GetMapping({"/walkin", "/{tenantName}/walkin"})
    public String showForm(@org.springframework.web.bind.annotation.PathVariable(value = "tenantName", required = false) String tenantName, @RequestParam(required = false) Long jobId, Model model) {
        com.stie.model.Tenant tenant = null;
        if (tenantName != null) {
            tenant = tenantService.getSiteBySubdomain(tenantName);
            if (tenant == null) {
                return "redirect:/login";
            }
        }
        model.addAttribute("tenant", tenant);
        model.addAttribute("candidate", new Candidate());
        model.addAttribute("branding", brandingService.getBranding(tenant));
        
        // Pass all active jobs across all sites to the view for the dropdown
        model.addAttribute("activeJobs", jobService.getAllOpenJobsAcrossTenants());
            
        if (jobId != null) {
            model.addAttribute("applyingJob", jobService.getJobById(jobId));
        }
        return "walkin";
    }

    @PostMapping({"/walkin/submit", "/{tenantName}/walkin/submit"})
    public String submitApplication(@org.springframework.web.bind.annotation.PathVariable(value = "tenantName", required = false) String tenantName,
                                    @RequestParam(value = "resume", required = false) MultipartFile resume,
                                    @RequestParam(value = "photo", required = false) MultipartFile photo,
                                    @RequestParam(value = "academicCert", required = false) MultipartFile academicCert,
                                    @RequestParam(value = "otherDoc", required = false) MultipartFile otherDoc,
                                    @RequestParam(value = "educationRaw", required = false) String educationRaw,
                                    @RequestParam(value = "certificationsRaw", required = false) String certificationsRaw,
                                    @RequestParam(value = "jobId", required = false) Long jobId,
                                    Candidate candidate, Model model) {
        com.stie.model.Tenant tenant = null;
        if (tenantName != null) {
            tenant = tenantService.getSiteBySubdomain(tenantName);
            if (tenant == null) return "redirect:/login";
        }

        if (jobId != null) {
            com.stie.model.JobVacancy job = jobService.getJobById(jobId);
            candidate.setJobVacancy(job);
            if (tenant == null && job != null) {
                tenant = job.getTenant();
            }
        } else {
            model.addAttribute("error", "Please select a job vacancy.");
            model.addAttribute("candidate", candidate);
            model.addAttribute("activeJobs", jobService.getAllOpenJobsAcrossTenants());
            return "walkin";
        }

        model.addAttribute("tenant", tenant);
        model.addAttribute("branding", brandingService.getBranding(tenant));
        candidate.setTenant(tenant);
        
        if (resume == null || resume.isEmpty()) {
            model.addAttribute("error", "Resume upload is mandatory.");
            model.addAttribute("candidate", candidate);
            model.addAttribute("activeJobs", jobService.getAllOpenJobsAcrossTenants());
            return "walkin";
        }
        
        String uploadDir = com.stie.config.AppConstants.FilePaths.UPLOAD_DIR;
        Path uploadPath = Paths.get(uploadDir);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (photo != null && !photo.isEmpty()) {
            try (java.io.InputStream is = photo.getInputStream()) {
                String fileName = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                candidate.setPhotoPath(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (academicCert != null && !academicCert.isEmpty()) {
            try (java.io.InputStream is = academicCert.getInputStream()) {
                String fileName = UUID.randomUUID().toString() + "_" + academicCert.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                candidate.setAcademicCertPath(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (otherDoc != null && !otherDoc.isEmpty()) {
            try (java.io.InputStream is = otherDoc.getInputStream()) {
                String fileName = UUID.randomUUID().toString() + "_" + otherDoc.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                candidate.setOtherDocPath(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (resume != null && !resume.isEmpty()) {
            try (java.io.InputStream is = resume.getInputStream()) {
                String fileName = UUID.randomUUID().toString() + "_" + resume.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                candidate.setResumePath(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        try {
            // Check if email already exists
            if (candidate.getEmail() != null && !candidate.getEmail().trim().isEmpty()) {
                String email = candidate.getEmail().trim().toLowerCase();
                com.stie.model.Tenant finalTenant = tenant;
                boolean emailExists = candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent().stream()
                        .anyMatch(c -> email.equals(c.getEmail().trim().toLowerCase()) && 
                                       (finalTenant == null ? c.getTenant() == null : (c.getTenant() != null && finalTenant.getId().equals(c.getTenant().getId()))));
                if (emailExists) {
                    model.addAttribute("error", "An application with this email address ('" + candidate.getEmail() + "') already exists.");
                    model.addAttribute("candidate", candidate);
                    return "walkin";
                }
            }

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
            notificationService.addNotification("New candidate application received: " + candidate.getFullName() + " (" + candidate.getNationality() + ")", "/candidates/" + candidate.getId());
            notificationService.sendAcknowledgment(candidate.getEmail(), candidate.getFullName());
            model.addAttribute("message", "Application submitted successfully! Our HR team will contact you soon.");
            return "success";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("error", "An application with this email address already exists.");
            model.addAttribute("candidate", candidate);
            return "walkin";
        } catch (Exception e) {
            model.addAttribute("error", "Error saving application: " + e.getMessage());
            model.addAttribute("candidate", candidate);
            return "walkin";
        }
    }
}


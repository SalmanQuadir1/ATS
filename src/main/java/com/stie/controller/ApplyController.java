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
    private ParserService parserService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BrandingService brandingService;

    @GetMapping("/apply/{id}")
    public String showApplyForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        System.out.println("DEBUG: showApplyForm called for job ID: " + id);
        JobVacancy job = jobService.getJobById(id);
        System.out.println("DEBUG: Job found: " + (job != null ? job.getTitle() : "null"));
        if (job == null) {
            ra.addFlashAttribute("error", "Job vacancy not found with ID: " + id);
            return "redirect:/landing";
        }
        model.addAttribute("branding", brandingService.getBranding());
        model.addAttribute("job", job);
        model.addAttribute("candidate", new Candidate());
        return "apply";
    }

    @PostMapping("/apply/{id}/submit")
    public String submitApplication(@PathVariable Long id,
                                    @RequestParam(value = "resume", required = false) MultipartFile resume,
                                    @RequestParam(value = "photo", required = false) MultipartFile photo,
                                    Candidate candidate,
                                    Model model) {
        System.out.println("DEBUG: submitApplication called for job ID: " + id);
        System.out.println("DEBUG: Candidate name: " + candidate.getFullName() + ", email: " + candidate.getEmail());

        JobVacancy job = jobService.getJobById(id);
        System.out.println("DEBUG: Job found: " + (job != null ? job.getTitle() : "null"));

        if (job == null) {
            model.addAttribute("error", "Job vacancy not found with ID: " + id);
            model.addAttribute("branding", brandingService.getBranding());
            return "apply";
        }

        model.addAttribute("branding", brandingService.getBranding());
        model.addAttribute("job", job);
        candidate.setJobVacancy(job);

        String uploadDir = "uploads/";
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
                Files.copy(resume.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                candidate.setResumePath(resumeFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (photo != null && !photo.isEmpty()) {
            try {
                String photoFileName = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
                Path filePath = uploadPath.resolve(photoFileName);
                Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                candidate.setPhotoPath(photoFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (candidate.getEmail() != null && !candidate.getEmail().trim().isEmpty()) {
                String email = candidate.getEmail().trim().toLowerCase();
                boolean emailExists = candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent().stream()
                        .anyMatch(c -> email.equals(c.getEmail().trim().toLowerCase()));
                if (emailExists) {
                    model.addAttribute("error", "An application with this email address ('" + candidate.getEmail() + "') already exists.");
                    model.addAttribute("candidate", candidate);
                    return "apply";
                }
            }
            candidateService.saveCandidate(candidate);
            notificationService.addNotification("New application for " + job.getTitle() + ": " + candidate.getFullName(), "/candidates/" + candidate.getId());
            notificationService.sendAcknowledgment(candidate.getEmail(), candidate.getFullName());
            model.addAttribute("message", "Application for " + job.getTitle() + " submitted successfully! Our HR team will contact you soon.");
            return "success";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("error", "An application with this email address already exists.");
            model.addAttribute("candidate", candidate);
            return "apply";
        } catch (Exception e) {
            model.addAttribute("error", "Error saving application: " + e.getMessage());
            model.addAttribute("candidate", candidate);
            return "apply";
        }
    }
}


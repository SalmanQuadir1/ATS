package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.service.CandidateService;
import com.stie.service.NotificationService;
import com.stie.service.ParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class UnifiedWebhookController {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private ParserService parserService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.stie.service.AuditService auditService;

    /**
     * LinkedIn Easy Apply integration endpoint.
     */
    @PostMapping("/linkedin/apply")
    public ResponseEntity<Map<String, Object>> handleLinkedInApply(
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "nationality", required = false) String nationality,
            @RequestParam(value = "experienceYears", required = false) Integer experienceYears) {
        
        return processApplication("LINKEDIN", resume, fullName, email, phone, nationality, experienceYears);
    }

    /**
     * JobStreet Direct Apply integration endpoint.
     */
    @PostMapping("/jobstreet/apply")
    public ResponseEntity<Map<String, Object>> handleJobStreetApply(
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "nationality", required = false) String nationality,
            @RequestParam(value = "experienceYears", required = false) Integer experienceYears) {
        
        return processApplication("JOBSTREET", resume, fullName, email, phone, nationality, experienceYears);
    }

    /**
     * Indeed Apply integration endpoint.
     */
    @PostMapping("/indeed/apply")
    public ResponseEntity<Map<String, Object>> handleIndeedApply(
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "nationality", required = false) String nationality,
            @RequestParam(value = "experienceYears", required = false) Integer experienceYears) {
        
        return processApplication("INDEED", resume, fullName, email, phone, nationality, experienceYears);
    }

    /**
     * Unified processor for multi-platform incoming applications.
     */
    private ResponseEntity<Map<String, Object>> processApplication(
            String platform,
            MultipartFile resume,
            String fullName,
            String email,
            String phone,
            String nationality,
            Integer experienceYears) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Check for duplicate application (email uniqueness)
            String cleanEmail = email.trim().toLowerCase();
            boolean emailExists = candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent().stream()
                    .anyMatch(c -> cleanEmail.equals(c.getEmail().trim().toLowerCase()));

            if (emailExists) {
                response.put("success", false);
                response.put("message", "Application with email '" + email + "' already exists in our database.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            Candidate candidate = new Candidate();
            candidate.setFullName(fullName);
            candidate.setEmail(email);
            candidate.setPhone(phone != null ? phone : "");
            candidate.setNationality(nationality != null ? nationality : "Foreigner");
            candidate.setExperienceYears(experienceYears != null ? experienceYears : 0);
            candidate.setStatus(Candidate.CandidateStatus.APPLIED);

            // If a resume is uploaded inline, parse it to extract rich metadata (Skills, Education, etc.)
            if (resume != null && !resume.isEmpty()) {
                Candidate parsedData = parserService.parseResume(resume);
                
                // Merge details parsed from resume if fields are not provided in request
                if (candidate.getFullName() == null || candidate.getFullName().isEmpty()) {
                    candidate.setFullName(parsedData.getFullName());
                }
                if (candidate.getPhone() == null || candidate.getPhone().isEmpty()) {
                    candidate.setPhone(parsedData.getPhone());
                }
                if (parsedData.getEducation() != null && !parsedData.getEducation().isEmpty()) {
                    candidate.setEducation(parsedData.getEducation());
                }
                if (parsedData.getSkills() != null && !parsedData.getSkills().isEmpty()) {
                    candidate.setSkills(parsedData.getSkills());
                }
                if (parsedData.getNationality() != null && !"Foreigner".equalsIgnoreCase(parsedData.getNationality())) {
                    candidate.setNationality(parsedData.getNationality());
                }
                if (parsedData.getExperienceYears() != null && parsedData.getExperienceYears() > 0) {
                    candidate.setExperienceYears(parsedData.getExperienceYears());
                }
            }

            // Save the applicant
            candidateService.saveCandidate(candidate);

            // Log audit trail
            auditService.log(platform + "_EASY_APPLY", platform + "_WEBHOOK", "Candidate", candidate.getId(), 
                "Ingested application from " + platform + " for candidate: " + candidate.getFullName());

            // Send acknowledgment
            notificationService.sendAcknowledgment(candidate.getEmail(), candidate.getFullName());

            response.put("success", true);
            response.put("candidateId", candidate.getId());
            response.put("platform", platform);
            response.put("message", platform + " application processed successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Dedicated Email Forwarding Ingestion integration webhook.
     */
    @PostMapping("/email/resume")
    public ResponseEntity<Map<String, Object>> handleEmailIngest(
            @RequestParam("fromEmail") String fromEmail,
            @RequestParam("subject") String subject,
            @RequestParam(value = "body", required = false) String body,
            @RequestParam(value = "resume", required = false) MultipartFile resume) {

        Map<String, Object> response = new HashMap<>();

        try {
            String cleanEmail = fromEmail.trim().toLowerCase();
            
            // Extract a clean name from the email prefix if not parsed from resume
            String defaultName = fromEmail.split("@")[0];
            defaultName = defaultName.substring(0, 1).toUpperCase() + (defaultName.length() > 1 ? defaultName.substring(1) : "");

            // Pre-check duplicate email
            boolean emailExists = candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent().stream()
                    .anyMatch(c -> cleanEmail.equals(c.getEmail().trim().toLowerCase()));

            if (emailExists) {
                response.put("success", false);
                response.put("message", "Forwarded email candidate with address '" + fromEmail + "' already exists in database.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            Candidate candidate = new Candidate();
            candidate.setFullName(defaultName);
            candidate.setEmail(fromEmail);
            candidate.setPhone("");
            candidate.setNationality("Foreigner");
            candidate.setExperienceYears(1);
            candidate.setStatus(Candidate.CandidateStatus.APPLIED);
            candidate.setEducation("Extracted from Forwarded Email System");
            candidate.setSkills("Forwarded");

            // Parse attached CV
            if (resume != null && !resume.isEmpty()) {
                Candidate parsedData = parserService.parseResume(resume);
                
                if (parsedData.getFullName() != null && !parsedData.getFullName().isEmpty()) {
                    candidate.setFullName(parsedData.getFullName());
                }
                if (parsedData.getPhone() != null && !parsedData.getPhone().isEmpty()) {
                    candidate.setPhone(parsedData.getPhone());
                }
                if (parsedData.getEducation() != null && !parsedData.getEducation().isEmpty()) {
                    candidate.setEducation(parsedData.getEducation());
                }
                if (parsedData.getSkills() != null && !parsedData.getSkills().isEmpty()) {
                    candidate.setSkills(parsedData.getSkills());
                }
                if (parsedData.getNationality() != null && !"Foreigner".equalsIgnoreCase(parsedData.getNationality())) {
                    candidate.setNationality(parsedData.getNationality());
                }
                if (parsedData.getExperienceYears() != null && parsedData.getExperienceYears() > 0) {
                    candidate.setExperienceYears(parsedData.getExperienceYears());
                }
            } else if (body != null && !body.trim().isEmpty()) {
                // Parse simple skills/metadata from email body text if no CV attached
                List<String> detectedSkills = new ArrayList<>();
                String lowerBody = body.toLowerCase();
                String[] commonSkills = {"java", "spring", "react", "angular", "python", "aws", "docker", "mysql", "excel", "security"};
                for (String skill : commonSkills) {
                    if (lowerBody.contains(skill)) {
                        detectedSkills.add(skill.substring(0, 1).toUpperCase() + skill.substring(1));
                    }
                }
                if (!detectedSkills.isEmpty()) {
                    candidate.setSkills(String.join(", ", detectedSkills));
                }
            }

            candidateService.saveCandidate(candidate);

            auditService.log("EMAIL_FORWARD_INGEST", "Email_Webhook", "Candidate", candidate.getId(), 
                "Ingested resume forward from email: " + fromEmail + " with subject: " + subject);

            notificationService.sendAcknowledgment(candidate.getEmail(), candidate.getFullName());

            response.put("success", true);
            response.put("candidateId", candidate.getId());
            response.put("message", "Resume ingestion from forwarded email processed successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}


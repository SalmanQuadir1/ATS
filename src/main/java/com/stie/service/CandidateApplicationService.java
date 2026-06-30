package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.CandidateApplication;
import com.stie.model.CandidateApplication.AppStatus;
import com.stie.model.CandidateApplication.ApplicationSource;
import com.stie.model.JobVacancy;
import com.stie.repository.CandidateApplicationRepository;
import com.stie.repository.CandidateRepository;
import com.stie.repository.JobVacancyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CandidateApplicationService {

    @Autowired
    private CandidateApplicationRepository applicationRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobVacancyRepository jobVacancyRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all applications for a candidate, newest first.
     */
    public List<CandidateApplication> getApplicationsForCandidate(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .map(applicationRepository::findByCandidateOrderByAppliedAtDesc)
                .orElse(java.util.Collections.emptyList());
    }

    /**
     * Get all applications for a given job vacancy.
     */
    public List<CandidateApplication> getApplicationsForJob(Long jobId) {
        return jobVacancyRepository.findById(jobId)
                .map(applicationRepository::findByJobVacancy)
                .orElse(java.util.Collections.emptyList());
    }

    /**
     * Count total applications per candidate.
     */
    public long countApplicationsForCandidate(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .map(applicationRepository::countByCandidate)
                .orElse(0L);
    }

    /**
     * Create a new application record, linking a candidate to a job.
     * Returns empty Optional if the candidate already applied to this job.
     */
    public Optional<CandidateApplication> createApplication(
            Long candidateId,
            Long jobVacancyId,
            ApplicationSource source,
            String location,
            String notes,
            String performedBy) {

        Candidate candidate = candidateRepository.findById(candidateId).orElse(null);
        if (candidate == null) return Optional.empty();

        JobVacancy job = (jobVacancyId != null)
                ? jobVacancyRepository.findById(jobVacancyId).orElse(null)
                : null;

        // Prevent duplicate application for same job (if job is specified)
        if (job != null) {
            Optional<CandidateApplication> existing =
                    applicationRepository.findByCandidateAndJobVacancy(candidate, job);
            if (existing.isPresent()) return Optional.empty();
        }

        // PRESERVE legacy application before creating new one or changing active job
        if (job != null && candidate.getJobVacancy() != null && !candidate.getJobVacancy().getId().equals(job.getId())) {
            seedLegacyApplicationIfAbsent(candidate);
        }

        CandidateApplication app = new CandidateApplication();
        app.setCandidate(candidate);
        app.setJobVacancy(job);
        app.setSource(source != null ? source : ApplicationSource.ONLINE);
        app.setLocation(location);
        app.setNotes(notes);
        app.setStatus(AppStatus.APPLIED);
        
        String year = String.valueOf(java.time.Year.now().getValue());
        String randomStr = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        app.setApplicationId("APP-" + year + "-" + randomStr);

        CandidateApplication saved = applicationRepository.save(app);

        // Update candidate's overall active job to the latest application
        if (job != null && (candidate.getJobVacancy() == null || !candidate.getJobVacancy().getId().equals(job.getId()))) {
            candidate.setJobVacancy(job);
            candidateRepository.save(candidate);
        }

        auditService.log("APPLICATION_CREATED", performedBy, "CandidateApplication", saved.getId(),
                "Application created for candidate: " + candidate.getFullName()
                + (job != null ? " | Job: " + job.getTitle() : " | No specific job"));
        return Optional.of(saved);
    }

    /**
     * Update the status of a specific application.
     */
    public boolean updateApplicationStatus(Long applicationId, AppStatus newStatus, String performedBy) {
        Optional<CandidateApplication> optApp = applicationRepository.findById(applicationId);
        if (!optApp.isPresent()) return false;

        CandidateApplication app = optApp.get();
        AppStatus oldStatus = app.getStatus();
        
        if (oldStatus == AppStatus.SHORTLISTED && 
           (newStatus == AppStatus.OFFERED || newStatus == AppStatus.HIRED)) {
            throw new IllegalArgumentException("Cannot transition directly from SHORTLISTED to OFFERED or HIRED without an INTERVIEW.");
        }

        app.setStatus(newStatus);
        applicationRepository.save(app);

        auditService.log("APPLICATION_STATUS_CHANGE", performedBy, "CandidateApplication", applicationId,
                "Status: " + oldStatus + " → " + newStatus
                + " | Candidate: " + app.getCandidate().getFullName()
                + (app.getJobVacancy() != null ? " | Job: " + app.getJobVacancy().getTitle() : ""));

        // Send email notification to candidate about status change
        String candidateEmail = app.getCandidate().getEmail();
        String candidateName  = app.getCandidate().getFullName();
        String jobTitle       = app.getJobVacancy() != null ? app.getJobVacancy().getTitle() : null;
        notificationService.sendApplicationStatusUpdateEmail(candidateEmail, candidateName, jobTitle, newStatus);

        return true;
    }

    /**
     * Find a specific application by ID.
     */
    public Optional<CandidateApplication> findById(Long applicationId) {
        return applicationRepository.findById(applicationId);
    }

    /**
     * Seed an application record from the legacy Candidate.jobVacancy FK.
     * Called when viewing a candidate who has a jobVacancy linked but no
     * CandidateApplication record yet (backward-compat migration).
     */
    public void seedLegacyApplicationIfAbsent(Candidate candidate) {
        if (candidate.getJobVacancy() == null) return;
        Optional<CandidateApplication> existing =
                applicationRepository.findByCandidateAndJobVacancy(candidate, candidate.getJobVacancy());
        if (existing.isPresent()) return;

        CandidateApplication app = new CandidateApplication();
        app.setCandidate(candidate);
        app.setJobVacancy(candidate.getJobVacancy());
        app.setSource(ApplicationSource.ONLINE);
        app.setAppliedAt(candidate.getAppliedAt() != null ? candidate.getAppliedAt() : java.time.LocalDateTime.now());
        // Map legacy CandidateStatus to AppStatus
        try {
            AppStatus mapped = AppStatus.valueOf(candidate.getStatus().name());
            app.setStatus(mapped);
        } catch (Exception e) {
            app.setStatus(AppStatus.APPLIED);
        }
        applicationRepository.save(app);
    }
}

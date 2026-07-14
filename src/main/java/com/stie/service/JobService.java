package com.stie.service;

import com.stie.model.JobVacancy;
import com.stie.model.Tenant;
import com.stie.repository.JobVacancyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {

    @Autowired
    private JobVacancyRepository repository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserService userService;

    public List<JobVacancy> getAllVacancies() {
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        if (tenant != null) {
            return repository.findByTenant(tenant);
        }
        return repository.findAll();
    }

    public List<JobVacancy> getAllVacanciesAcrossTenants() {
        return repository.findAll();
    }

    public JobVacancy saveVacancy(JobVacancy vacancy) {
        if (vacancy.getTenant() == null) {
            vacancy.setTenant(userService.getCurrentTenant());
        }
        return repository.save(vacancy);
    }

    public void approveVacancy(Long id) {
        approveVacancy(id, "Approved by Admin", "System");
    }

    public void approveVacancy(Long id, String note, String username) {
        Tenant tenant = userService.getCurrentTenant();
        repository.findById(id).filter(v -> tenant == null || v.getTenant() == null || tenant.getId().equals(v.getTenant().getId())).ifPresent(v -> {
            v.setApprovalStatus(JobVacancy.ApprovalStatus.APPROVED);
            v.setStatus(JobVacancy.JobStatus.OPEN);
            v.setApprovedByManager(true);
            v.setApprovalNote(note);
            v.setApprovedByUser(username);
            repository.save(v);
            auditService.log("JOB_APPROVED", username, "JobVacancy", id, "Approved job: " + v.getTitle() + " - Note: " + note);
        });
    }

    public void rejectVacancy(Long id, String note, String username) {
        Tenant tenant = userService.getCurrentTenant();
        repository.findById(id).filter(v -> tenant == null || v.getTenant() == null || tenant.getId().equals(v.getTenant().getId())).ifPresent(v -> {
            v.setApprovalStatus(JobVacancy.ApprovalStatus.REJECTED);
            v.setStatus(JobVacancy.JobStatus.CLOSED);
            v.setApprovedByManager(false);
            v.setApprovalNote(note);
            v.setApprovedByUser(username);
            repository.save(v);
            auditService.log("JOB_REJECTED", username, "JobVacancy", id, "Rejected job: " + v.getTitle() + " - Note: " + note);
        });
    }


    public List<JobVacancy> getPendingJobs() {
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        if (tenant != null) {
            return repository.findByTenantAndApprovalStatus(tenant, JobVacancy.ApprovalStatus.PENDING);
        }
        return repository.findByApprovalStatus(JobVacancy.ApprovalStatus.PENDING);
    }

    public List<JobVacancy> getOpenJobs() {
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        if (tenant != null) {
            return getOpenJobsForTenant(tenant);
        }
        return repository.findAll().stream()
                .filter(j -> j.getStatus() == JobVacancy.JobStatus.OPEN
                          && j.getApprovalStatus() == JobVacancy.ApprovalStatus.APPROVED
                          && j.isActive() != null && j.isActive())
                .collect(Collectors.toList());
    }

    public List<JobVacancy> getAllOpenJobsAcrossTenants() {
        return repository.findAll().stream()
                .filter(j -> j.getStatus() == JobVacancy.JobStatus.OPEN
                          && j.getApprovalStatus() == JobVacancy.ApprovalStatus.APPROVED
                          && j.isActive() != null && j.isActive())
                .collect(Collectors.toList());
    }

    public List<JobVacancy> getOpenJobsForTenant(Tenant tenant) {
        if (tenant == null) return getOpenJobs();
        return repository.findByTenant(tenant).stream()
                .filter(j -> j.getStatus() == JobVacancy.JobStatus.OPEN
                          && j.getApprovalStatus() == JobVacancy.ApprovalStatus.APPROVED
                          && j.isActive() != null && j.isActive())
                .collect(Collectors.toList());
    }

    public List<JobVacancy> getAllApprovedJobsForTenant(Tenant tenant) {
        if (tenant == null) return java.util.Collections.emptyList();
        return repository.findByTenant(tenant).stream()
                .filter(j -> j.getApprovalStatus() == JobVacancy.ApprovalStatus.APPROVED)
                .collect(Collectors.toList());
    }

    public JobVacancy getJobById(Long id) {
        Tenant tenant = userService.getCurrentTenant();
        System.out.println("DEBUG: JobService.getJobById called with id: " + id + ", tenant: " + (tenant != null ? tenant.getId() : "null"));
        return repository.findById(id)
                .filter(v -> {
                    if (tenant == null) {
                        System.out.println("DEBUG: No tenant context, returning job with id: " + v.getId());
                        return true;
                    }
                    if (v.getTenant() == null) {
                        System.out.println("DEBUG: Job has no tenant, returning job with id: " + v.getId());
                        return true;
                    }
                    boolean match = tenant.getId() != null && tenant.getId().equals(v.getTenant().getId());
                    System.out.println("DEBUG: Tenant match result: " + match);
                    return match;
                })
                .orElse(null);
    }

    public JobVacancy getJobByIdAcrossTenants(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    public void checkExpiries() {
        LocalDateTime now = LocalDateTime.now();
        List<JobVacancy> expiredJobs = repository.findAll().stream()
                .filter(v -> v.getStatus() == JobVacancy.JobStatus.OPEN && v.getExpiryDate() != null && v.getExpiryDate().isBefore(now))
                .collect(Collectors.toList());

        for (JobVacancy job : expiredJobs) {
            job.setStatus(JobVacancy.JobStatus.CLOSED);
            repository.save(job);
            auditService.log("JOB_EXPIRED", "System", "JobVacancy", job.getId(), "Auto-closed job due to expiry: " + job.getTitle());
        }
    }
}


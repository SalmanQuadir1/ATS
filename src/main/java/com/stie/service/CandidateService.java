package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.Tenant;
import com.stie.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidateService {

    @Autowired
    private CandidateRepository repository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private com.stie.repository.JobVacancyRepository jobVacancyRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    public Page<Candidate> getAllCandidates(Pageable pageable) {
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        if (tenant != null && tenant.getId() != null) {
            return repository.findByTenant(tenant, pageable);
        }
        return repository.findAll(pageable);
    }
    
    public Page<Candidate> getApplications(Pageable pageable) {
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        if (tenant != null && tenant.getId() != null) {
            return repository.findByTenantAndJobVacancyIsNotNull(tenant, pageable);
        }
        return repository.findByJobVacancyIsNotNull(pageable);
    }

    public Candidate saveCandidate(Candidate candidate) {
        if (candidate.getTenant() == null) {
            candidate.setTenant(userService.getCurrentTenant());
        }
        if (candidate.getApplicationId() == null) {
            String year = String.valueOf(java.time.Year.now().getValue());
            String appId;
            do {
                String randomStr = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                appId = "APP-" + year + "-" + randomStr;
            } while (repository.findByApplicationId(appId).isPresent());
            candidate.setApplicationId(appId);
        }
        Candidate saved = repository.save(candidate);
        auditService.log("CANDIDATE_APPLIED", "Public/Walk-in", "Candidate", saved.getId(), "New application: " + saved.getFullName());
        return saved;
    }

    public void updateStatus(Long id, Candidate.CandidateStatus status, String performedBy) {
        Tenant tenant = userService.getCurrentTenant();
        repository.findById(id).filter(c -> tenant == null || c.getTenant() == null || (tenant.getId() != null && tenant.getId().equals(c.getTenant().getId()))).ifPresent(c -> {
            if (c.getStatus() == Candidate.CandidateStatus.SHORTLISTED && 
               (status == Candidate.CandidateStatus.OFFERED || status == Candidate.CandidateStatus.HIRED)) {
                throw new IllegalArgumentException("Cannot transition directly from SHORTLISTED to OFFERED or HIRED without an INTERVIEW.");
            }
            c.setStatus(status);
            repository.save(c);
            auditService.log("CANDIDATE_STATUS_UPDATED", performedBy != null ? performedBy : "System", "Candidate", id, "Status changed to: " + status);
            
            if (status == Candidate.CandidateStatus.REJECTED) {
                notificationService.sendRejectionEmail(c.getEmail(), c.getFullName());
            }
        });
    }

    public void updateSharedWithHM(Long id, boolean shared) {
        Tenant tenant = userService.getCurrentTenant();
        repository.findById(id).filter(c -> tenant == null || c.getTenant() == null || (tenant.getId() != null && tenant.getId().equals(c.getTenant().getId()))).ifPresent(c -> {
            c.setSharedWithHM(shared);
            repository.save(c);
        });
    }

    public void assignInterviewer(Long id, com.stie.model.User interviewer) {
        Tenant tenant = userService.getCurrentTenant();
        repository.findById(id).filter(c -> tenant == null || c.getTenant() == null || (tenant.getId() != null && tenant.getId().equals(c.getTenant().getId()))).ifPresent(c -> {
            c.setAssignedInterviewer(interviewer);
            repository.save(c);
        });
    }

    public java.util.Optional<Candidate> findById(Long id) {
        Tenant tenant = userService.getCurrentTenant();
        return repository.findById(id)
                .filter(c -> tenant == null || c.getTenant() == null || (tenant.getId() != null && tenant.getId().equals(c.getTenant().getId())));
    }

    public List<Candidate> search(String query, String nationality, Integer minExp, Integer maxSalary, String certifications, Boolean hasSecurityLicense, Boolean workPermitEligible, Long rankJobId, Long jobId, String status) {
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        List<Candidate> candidates = (tenant != null && tenant.getId() != null) ? repository.findByTenant(tenant) : repository.findAll();
        
        com.stie.model.JobVacancy rankJob = (rankJobId != null && rankJobId > 0) ? jobVacancyRepository.findById(rankJobId).orElse(null) : null;
        
        return candidates.stream()
                .filter(c -> jobId == null || (c.getJobVacancy() != null && c.getJobVacancy().getId().equals(jobId)))
                .filter(c -> status == null || status.isEmpty() || (c.getStatus() != null && c.getStatus().name().equalsIgnoreCase(status)))
                .filter(c -> query == null || query.isEmpty() || 
                             c.getFullName().toLowerCase().contains(query.toLowerCase()) || 
                             (c.getApplicationId() != null && c.getApplicationId().toLowerCase().contains(query.toLowerCase())) ||
                             (c.getSkills() != null && c.getSkills().toLowerCase().contains(query.toLowerCase())) ||
                             (c.getEducations() != null && c.getEducations().stream().anyMatch(e -> (e.getDegree() != null && e.getDegree().toLowerCase().contains(query.toLowerCase())) || (e.getInstitution() != null && e.getInstitution().toLowerCase().contains(query.toLowerCase())))) ||
                             (c.getTaggedRoles() != null && c.getTaggedRoles().toLowerCase().contains(query.toLowerCase())))
                .filter(c -> nationality == null || nationality.isEmpty() || (c.getNationality() != null && c.getNationality().equalsIgnoreCase(nationality)))
                .filter(c -> minExp == null || (c.getExperienceYears() != null && c.getExperienceYears() >= minExp))
                .filter(c -> maxSalary == null || c.getExpectedSalary() == null || c.getExpectedSalary() <= maxSalary)
                .filter(c -> certifications == null || certifications.isEmpty() || 
                             (c.getCertifications() != null && c.getCertifications().stream().anyMatch(cert -> cert.toLowerCase().contains(certifications.toLowerCase()))))
                .filter(c -> hasSecurityLicense == null || !hasSecurityLicense || 
                             (c.getSecurityLicense() != null && !c.getSecurityLicense().trim().isEmpty()))
                .filter(c -> workPermitEligible == null || !workPermitEligible || c.isWorkPermitEligible())
                .peek(c -> {
                    if (rankJob != null) {
                        c.setMatchScore(calculateMatchScore(c, rankJob));
                    } else {
                        c.setMatchScore(0);
                    }
                })
                .sorted((c1, c2) -> {
                    if (rankJob != null) {
                        return Integer.compare(c2.getMatchScore(), c1.getMatchScore()); // Match rank desc
                    }
                    return Long.compare(c2.getId(), c1.getId()); // Default newest first
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private int calculateMatchScore(Candidate candidate, com.stie.model.JobVacancy job) {
        int score = 0;
        if (candidate.getSkills() == null || candidate.getSkills().isEmpty()) return 0;
        
        String jobText = (job.getTitle() + " " + job.getDescription()).toLowerCase();
        String[] skills = candidate.getSkills().split(",");
        int matches = 0;
        for (String skill : skills) {
            String trimmedSkill = skill.trim().toLowerCase();
            if (!trimmedSkill.isEmpty() && jobText.contains(trimmedSkill)) {
                score += 15; // 15 points per matched skill
                matches++;
            }
        }
        
        // Experience contribution (up to 25 points)
        if (candidate.getExperienceYears() != null) {
            score += Math.min(candidate.getExperienceYears() * 3, 25);
        }
        
        // Normalize out of 100
        int finalPct = Math.min((score * 100) / (Math.max(1, matches) * 15 + 25), 100);
        return finalPct > 0 ? finalPct : 10; // Floor of 10% for basic details
    }
}


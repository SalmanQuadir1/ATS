package com.stie.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private Integer experienceYears;
    private String skills;
    private String education;
    private String nationality; // Singaporean, PR, Foreigner
    
    @Enumerated(EnumType.STRING)
    private CandidateStatus status = CandidateStatus.APPLIED;

    private String passportNumber; // Encrypted in production
    private boolean workPermitEligible;
    private String securityLicense; // SOP point 3 requirement
    private Integer expectedSalary; // For salary filtering
    private String certifications; // For certifications filtering
    private String taggedRoles; // For matching future/past opportunities
    private boolean sharedWithHM = false; // Workflow status
    
    private String photoPath;
    private String resumePath;
    
    @Column(length = 2000)
    private String projects;
    private String projectUrl;

    @Transient
    private Integer matchScore = 0; // Runtime transient field for skill matching rank

    private LocalDateTime appliedAt;

    public Candidate() {
        this.appliedAt = LocalDateTime.now();
        this.sharedWithHM = false;
        this.matchScore = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public CandidateStatus getStatus() { return status; }
    public void setStatus(CandidateStatus status) { this.status = status; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public boolean isWorkPermitEligible() { return workPermitEligible; }
    public void setWorkPermitEligible(boolean workPermitEligible) { this.workPermitEligible = workPermitEligible; }

    public String getSecurityLicense() { return securityLicense; }
    public void setSecurityLicense(String securityLicense) { this.securityLicense = securityLicense; }

    public Integer getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(Integer expectedSalary) { this.expectedSalary = expectedSalary; }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }

    public boolean isSharedWithHM() { return sharedWithHM; }
    public void setSharedWithHM(boolean sharedWithHM) { this.sharedWithHM = sharedWithHM; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public String getTaggedRoles() { return taggedRoles; }
    public void setTaggedRoles(String taggedRoles) { this.taggedRoles = taggedRoles; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User candidateUser;

    @ManyToOne
    @JoinColumn(name = "job_vacancy_id")
    private JobVacancy jobVacancy;

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public User getCandidateUser() { return candidateUser; }
    public void setCandidateUser(User candidateUser) { this.candidateUser = candidateUser; }

    public JobVacancy getJobVacancy() { return jobVacancy; }
    public void setJobVacancy(JobVacancy jobVacancy) { this.jobVacancy = jobVacancy; }

    public enum CandidateStatus {
        APPLIED, SHORTLISTED, INTERVIEW, OFFERED, HIRED, REJECTED, KIV
    }
}


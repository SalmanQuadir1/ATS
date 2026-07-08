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

    @Column(nullable = false)
    private String email;

    private String phone;
    private Integer experienceYears;
    private String skills;

    @ElementCollection
    @CollectionTable(name = "candidate_educations", joinColumns = @JoinColumn(name = "candidate_id"))
    private java.util.List<CandidateEducation> educations = new java.util.ArrayList<>();

    private String nationality; // Singaporean, PR, Foreigner
    
    @Column(name = "application_id", unique = true)
    private String applicationId;
    
    
    @Enumerated(EnumType.STRING)
    private CandidateStatus status = CandidateStatus.APPLIED;

    private String passportNumber; // Encrypted in production
    private boolean workPermitEligible;
    private String securityLicense; // SOP point 3 requirement
    private Integer expectedSalary; // For salary filtering

    @ElementCollection
    @CollectionTable(name = "candidate_certifications", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "certification")
    private java.util.List<String> certifications = new java.util.ArrayList<>();

    private String taggedRoles; // For matching future/past opportunities
    private boolean sharedWithHM = false; // Workflow status
    
    private String photoPath;
    private String resumePath;
    private String academicCertPath;
    private String otherDocPath;
    
    @Column(length = 2000)
    private String projects;
    private String projectUrl;

    @Column(name = "transfer_source")
    private String transferSource;

    private String offerLetterPath;

    @Column(name = "signed_offer_letter_path")
    private String signedOfferLetterPath;

    @Column(name = "final_salary")
    private Double finalSalary;

    @Column(name = "joining_date")
    private java.time.LocalDate joiningDate;

    @Column(name = "hire_notes", length = 1000)
    private String hireNotes;

    @Column(name = "rejection_remarks", length = 1000)
    private String rejectionRemarks;

    @Transient
    private Integer matchScore = 0; // Runtime transient field for skill matching rank

    private LocalDateTime appliedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.appliedAt == null) {
            this.appliedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public java.util.List<CandidateEducation> getEducations() { return educations; }
    public void setEducations(java.util.List<CandidateEducation> educations) { this.educations = educations; }

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

    public java.util.List<String> getCertifications() { return certifications; }
    public void setCertifications(java.util.List<String> certifications) { this.certifications = certifications; }

    public boolean isSharedWithHM() { return sharedWithHM; }
    public void setSharedWithHM(boolean sharedWithHM) { this.sharedWithHM = sharedWithHM; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getTaggedRoles() { return taggedRoles; }
    public void setTaggedRoles(String taggedRoles) { this.taggedRoles = taggedRoles; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }

    public String getAcademicCertPath() { return academicCertPath; }
    public void setAcademicCertPath(String academicCertPath) { this.academicCertPath = academicCertPath; }

    public String getOtherDocPath() { return otherDocPath; }
    public void setOtherDocPath(String otherDocPath) { this.otherDocPath = otherDocPath; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    public String getOfferLetterPath() { return offerLetterPath; }
    public void setOfferLetterPath(String offerLetterPath) { this.offerLetterPath = offerLetterPath; }

    public String getSignedOfferLetterPath() { return signedOfferLetterPath; }
    public void setSignedOfferLetterPath(String signedOfferLetterPath) { this.signedOfferLetterPath = signedOfferLetterPath; }

    public Double getFinalSalary() { return finalSalary; }
    public void setFinalSalary(Double finalSalary) { this.finalSalary = finalSalary; }

    public java.time.LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(java.time.LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public String getHireNotes() { return hireNotes; }
    public void setHireNotes(String hireNotes) { this.hireNotes = hireNotes; }

    public String getRejectionRemarks() { return rejectionRemarks; }
    public void setRejectionRemarks(String rejectionRemarks) { this.rejectionRemarks = rejectionRemarks; }

    public String getTransferSource() { return transferSource; }
    public void setTransferSource(String transferSource) { this.transferSource = transferSource; }

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User candidateUser;

    @ManyToOne
    @JoinColumn(name = "job_vacancy_id")
    private JobVacancy jobVacancy;

    @ManyToOne
    @JoinColumn(name = "assigned_interviewer_id")
    private User assignedInterviewer;

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public User getCandidateUser() { return candidateUser; }
    public void setCandidateUser(User candidateUser) { this.candidateUser = candidateUser; }

    public JobVacancy getJobVacancy() { return jobVacancy; }
    public void setJobVacancy(JobVacancy jobVacancy) { this.jobVacancy = jobVacancy; }

    public User getAssignedInterviewer() { return assignedInterviewer; }
    public void setAssignedInterviewer(User assignedInterviewer) { this.assignedInterviewer = assignedInterviewer; }

    public enum CandidateStatus {
        APPLIED, SHORTLISTED, INTERVIEW, OFFERED, HIRED, REJECTED, KIV
    }
}


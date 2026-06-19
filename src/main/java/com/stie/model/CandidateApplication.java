package com.stie.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents one application by a Candidate to a specific JobVacancy.
 * A single Candidate (master profile) can have many CandidateApplications
 * across different jobs and over time.
 */
@Entity
@Table(name = "candidate_applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_id", "job_vacancy_id"}))
public class CandidateApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne
    @JoinColumn(name = "job_vacancy_id")
    private JobVacancy jobVacancy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppStatus status = AppStatus.APPLIED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationSource source = ApplicationSource.ONLINE;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    /** Branch / site / location where application was received */
    private String location;

    /** Internal HR remarks specific to this application */
    @Column(length = 2000)
    private String notes;

    public CandidateApplication() {
        this.appliedAt = LocalDateTime.now();
    }

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    public enum AppStatus {
        APPLIED, SHORTLISTED, INTERVIEW, OFFERED, HIRED, REJECTED, KIV
    }

    public enum ApplicationSource {
        ONLINE, WALK_IN, REFERRAL, DIRECT, PORTAL
    }

    // -----------------------------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public JobVacancy getJobVacancy() { return jobVacancy; }
    public void setJobVacancy(JobVacancy jobVacancy) { this.jobVacancy = jobVacancy; }

    public AppStatus getStatus() { return status; }
    public void setStatus(AppStatus status) { this.status = status; }

    public ApplicationSource getSource() { return source; }
    public void setSource(ApplicationSource source) { this.source = source; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

package com.stie.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_transfers")
public class CandidateTransferRequest {

    public enum TransferStatus {
        PENDING, PENDING_DESTINATION, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_job_vacancy_id")
    private JobVacancy fromJobVacancy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_job_vacancy_id", nullable = false)
    private JobVacancy toJobVacancy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "request_notes", columnDefinition = "TEXT")
    private String requestNotes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant; // Source Tenant

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_tenant_id")
    private Tenant targetTenant; // Destination Tenant

    @Column(name = "destination_reviewed_by")
    private String destinationReviewedBy;

    @Column(name = "destination_admin_remarks", columnDefinition = "TEXT")
    private String destinationAdminRemarks;

    @Column(name = "destination_reviewed_at")
    private LocalDateTime destinationReviewedAt;

    public CandidateTransferRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public JobVacancy getFromJobVacancy() { return fromJobVacancy; }
    public void setFromJobVacancy(JobVacancy fromJobVacancy) { this.fromJobVacancy = fromJobVacancy; }

    public JobVacancy getToJobVacancy() { return toJobVacancy; }
    public void setToJobVacancy(JobVacancy toJobVacancy) { this.toJobVacancy = toJobVacancy; }

    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getRequestNotes() { return requestNotes; }
    public void setRequestNotes(String requestNotes) { this.requestNotes = requestNotes; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Tenant getTargetTenant() { return targetTenant; }
    public void setTargetTenant(Tenant targetTenant) { this.targetTenant = targetTenant; }

    public String getDestinationReviewedBy() { return destinationReviewedBy; }
    public void setDestinationReviewedBy(String destinationReviewedBy) { this.destinationReviewedBy = destinationReviewedBy; }

    public String getDestinationAdminRemarks() { return destinationAdminRemarks; }
    public void setDestinationAdminRemarks(String destinationAdminRemarks) { this.destinationAdminRemarks = destinationAdminRemarks; }

    public LocalDateTime getDestinationReviewedAt() { return destinationReviewedAt; }
    public void setDestinationReviewedAt(LocalDateTime destinationReviewedAt) { this.destinationReviewedAt = destinationReviewedAt; }
}

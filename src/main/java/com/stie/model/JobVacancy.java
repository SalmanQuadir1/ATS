package com.stie.model;

import javax.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "job_vacancies")
public class JobVacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String department;

    private String location; // Free-text location (replaces old Site FK)
    @ManyToOne
    @JoinColumn(name = "category_id")
    private JobCategory category;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "job_vacancy_skills",
        joinColumns = @JoinColumn(name = "job_vacancy_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private LocalDateTime createdAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expiryDate;

    @Column(name = "no_of_posts", nullable = false)
    private Integer noOfPosts = 1;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "hiring_manager_id")
    private User hiringManager;

    private String approvalNote;

    private boolean approvedByManager = false;

    public JobVacancy() {
        this.createdAt = LocalDateTime.now();
    }

    public JobVacancy(String title, String description, String department) {
        this();
        this.title = title;
        this.description = description;
        this.department = department;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public JobCategory getCategory() { return category; }
    public void setCategory(JobCategory category) { this.category = category; }

    public Set<Skill> getSkills() { return skills; }
    public void setSkills(Set<Skill> skills) { this.skills = skills; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public Integer getNoOfPosts() { return noOfPosts; }
    public void setNoOfPosts(Integer noOfPosts) { this.noOfPosts = noOfPosts; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public User getHiringManager() { return hiringManager; }
    public void setHiringManager(User hiringManager) { this.hiringManager = hiringManager; }

    public String getApprovalNote() { return approvalNote; }
    public void setApprovalNote(String approvalNote) { this.approvalNote = approvalNote; }

    public boolean isApprovedByManager() { return approvedByManager; }
    public void setApprovedByManager(boolean approvedByManager) { this.approvedByManager = approvedByManager; }

    public enum JobStatus {
        DRAFT, OPEN, CLOSED
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}


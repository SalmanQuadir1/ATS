package com.stie.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_scorecards")
public class InterviewScorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(nullable = false)
    private Integer technicalScore = 3; // 1 to 5

    @Column(nullable = false)
    private Integer problemSolvingScore = 3; // 1 to 5

    @Column(nullable = false)
    private Integer communicationScore = 3; // 1 to 5

    @Column(nullable = false)
    private Integer cultureScore = 3; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(nullable = false)
    private String submitter; // username/email of interviewer

    private LocalDateTime createdAt = LocalDateTime.now();

    public InterviewScorecard() {}

    public InterviewScorecard(Interview interview, Integer technicalScore, Integer problemSolvingScore, 
                              Integer communicationScore, Integer cultureScore, String comments, String submitter) {
        this.interview = interview;
        this.technicalScore = technicalScore;
        this.problemSolvingScore = problemSolvingScore;
        this.communicationScore = communicationScore;
        this.cultureScore = cultureScore;
        this.comments = comments;
        this.submitter = submitter;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }

    public Integer getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(Integer technicalScore) { this.technicalScore = technicalScore; }

    public Integer getProblemSolvingScore() { return problemSolvingScore; }
    public void setProblemSolvingScore(Integer problemSolvingScore) { this.problemSolvingScore = problemSolvingScore; }

    public Integer getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(Integer communicationScore) { this.communicationScore = communicationScore; }

    public Integer getCultureScore() { return cultureScore; }
    public void setCultureScore(Integer cultureScore) { this.cultureScore = cultureScore; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Transient
    public Double getAverageScore() {
        return (technicalScore + problemSolvingScore + communicationScore + cultureScore) / 4.0;
    }
}


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

    // 1. Communication
    @Column(nullable = false) private Integer communicationScore = 3;
    @Column(columnDefinition = "TEXT") private String communicationRemarks;

    // 2. Job Knowledge & Abilities
    @Column(nullable = false) private Integer jobKnowledgeScore = 3;
    @Column(columnDefinition = "TEXT") private String jobKnowledgeRemarks;

    // 3. Response
    @Column(nullable = false) private Integer responseScore = 3;
    @Column(columnDefinition = "TEXT") private String responseRemarks;

    // 4. Attitude
    @Column(nullable = false) private Integer attitudeScore = 3;
    @Column(columnDefinition = "TEXT") private String attitudeRemarks;

    // 5. Initiative
    @Column(nullable = false) private Integer initiativeScore = 3;
    @Column(columnDefinition = "TEXT") private String initiativeRemarks;

    // 6. Personality
    @Column(nullable = false) private Integer personalityScore = 3;
    @Column(columnDefinition = "TEXT") private String personalityRemarks;

    // 7. Leadership
    @Column(nullable = false) private Integer leadershipScore = 3;
    @Column(columnDefinition = "TEXT") private String leadershipRemarks;

    // 8. Teamwork
    @Column(nullable = false) private Integer teamworkScore = 3;
    @Column(columnDefinition = "TEXT") private String teamworkRemarks;

    // 9. Appearance / Professional Image
    @Column(nullable = false) private Integer appearanceScore = 3;
    @Column(columnDefinition = "TEXT") private String appearanceRemarks;

    @Column(columnDefinition = "TEXT")
    private String comments; // General comments

    // HR & Recommendation Fields
    private String recommendation; // Yes, No, KIV
    private String positionOffer;
    private String commencementDate;
    private String project;
    private String employmentStatus; // Full Time, Contract Basis, Part Time
    private String salaryOfferBasic;
    private String salaryOfferTransport;
    private String salaryOfferMobile;
    private String salaryOfferOther;

    @Column(columnDefinition = "LONGTEXT") private String signature1;
    @Column(columnDefinition = "LONGTEXT") private String signature2;
    @Column(columnDefinition = "LONGTEXT") private String signature3;
    
    private String interviewer1Name;
    private String interviewer2Name;
    private String approverName;

    @Column(nullable = false)
    private String submitter; // username/email of interviewer

    private LocalDateTime createdAt = LocalDateTime.now();

    public InterviewScorecard() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }

    public Integer getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(Integer communicationScore) { this.communicationScore = communicationScore; }
    public String getCommunicationRemarks() { return communicationRemarks; }
    public void setCommunicationRemarks(String communicationRemarks) { this.communicationRemarks = communicationRemarks; }

    public Integer getJobKnowledgeScore() { return jobKnowledgeScore; }
    public void setJobKnowledgeScore(Integer jobKnowledgeScore) { this.jobKnowledgeScore = jobKnowledgeScore; }
    public String getJobKnowledgeRemarks() { return jobKnowledgeRemarks; }
    public void setJobKnowledgeRemarks(String jobKnowledgeRemarks) { this.jobKnowledgeRemarks = jobKnowledgeRemarks; }

    public Integer getResponseScore() { return responseScore; }
    public void setResponseScore(Integer responseScore) { this.responseScore = responseScore; }
    public String getResponseRemarks() { return responseRemarks; }
    public void setResponseRemarks(String responseRemarks) { this.responseRemarks = responseRemarks; }

    public Integer getAttitudeScore() { return attitudeScore; }
    public void setAttitudeScore(Integer attitudeScore) { this.attitudeScore = attitudeScore; }
    public String getAttitudeRemarks() { return attitudeRemarks; }
    public void setAttitudeRemarks(String attitudeRemarks) { this.attitudeRemarks = attitudeRemarks; }

    public Integer getInitiativeScore() { return initiativeScore; }
    public void setInitiativeScore(Integer initiativeScore) { this.initiativeScore = initiativeScore; }
    public String getInitiativeRemarks() { return initiativeRemarks; }
    public void setInitiativeRemarks(String initiativeRemarks) { this.initiativeRemarks = initiativeRemarks; }

    public Integer getPersonalityScore() { return personalityScore; }
    public void setPersonalityScore(Integer personalityScore) { this.personalityScore = personalityScore; }
    public String getPersonalityRemarks() { return personalityRemarks; }
    public void setPersonalityRemarks(String personalityRemarks) { this.personalityRemarks = personalityRemarks; }

    public Integer getLeadershipScore() { return leadershipScore; }
    public void setLeadershipScore(Integer leadershipScore) { this.leadershipScore = leadershipScore; }
    public String getLeadershipRemarks() { return leadershipRemarks; }
    public void setLeadershipRemarks(String leadershipRemarks) { this.leadershipRemarks = leadershipRemarks; }

    public Integer getTeamworkScore() { return teamworkScore; }
    public void setTeamworkScore(Integer teamworkScore) { this.teamworkScore = teamworkScore; }
    public String getTeamworkRemarks() { return teamworkRemarks; }
    public void setTeamworkRemarks(String teamworkRemarks) { this.teamworkRemarks = teamworkRemarks; }

    public Integer getAppearanceScore() { return appearanceScore; }
    public void setAppearanceScore(Integer appearanceScore) { this.appearanceScore = appearanceScore; }
    public String getAppearanceRemarks() { return appearanceRemarks; }
    public void setAppearanceRemarks(String appearanceRemarks) { this.appearanceRemarks = appearanceRemarks; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getPositionOffer() { return positionOffer; }
    public void setPositionOffer(String positionOffer) { this.positionOffer = positionOffer; }

    public String getCommencementDate() { return commencementDate; }
    public void setCommencementDate(String commencementDate) { this.commencementDate = commencementDate; }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    public String getSalaryOfferBasic() { return salaryOfferBasic; }
    public void setSalaryOfferBasic(String salaryOfferBasic) { this.salaryOfferBasic = salaryOfferBasic; }

    public String getSalaryOfferTransport() { return salaryOfferTransport; }
    public void setSalaryOfferTransport(String salaryOfferTransport) { this.salaryOfferTransport = salaryOfferTransport; }

    public String getSalaryOfferMobile() { return salaryOfferMobile; }
    public void setSalaryOfferMobile(String salaryOfferMobile) { this.salaryOfferMobile = salaryOfferMobile; }

    public String getSalaryOfferOther() { return salaryOfferOther; }
    public void setSalaryOfferOther(String salaryOfferOther) { this.salaryOfferOther = salaryOfferOther; }

    public String getSignature1() { return signature1; }
    public void setSignature1(String signature1) { this.signature1 = signature1; }

    public String getSignature2() { return signature2; }
    public void setSignature2(String signature2) { this.signature2 = signature2; }

    public String getSignature3() { return signature3; }
    public void setSignature3(String signature3) { this.signature3 = signature3; }

    public String getInterviewer1Name() { return interviewer1Name; }
    public void setInterviewer1Name(String interviewer1Name) { this.interviewer1Name = interviewer1Name; }

    public String getInterviewer2Name() { return interviewer2Name; }
    public void setInterviewer2Name(String interviewer2Name) { this.interviewer2Name = interviewer2Name; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Transient
    public Double getAverageScore() {
        return (communicationScore + jobKnowledgeScore + responseScore + attitudeScore + 
                initiativeScore + personalityScore + leadershipScore + teamworkScore + appearanceScore) / 9.0;
    }
}


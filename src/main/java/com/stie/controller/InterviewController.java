package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.Interview;
import com.stie.model.InterviewScorecard;
import com.stie.model.Salary;
import com.stie.repository.SalaryRepository;
import com.stie.repository.UserRepository;
import com.stie.service.CandidateService;
import com.stie.service.InterviewService;
import com.stie.service.JobService;
import com.stie.service.NotificationService;
import com.stie.service.ScorecardService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/interviews")
public class InterviewController {

    @Autowired private InterviewService interviewService;
    @Autowired private CandidateService candidateService;
    @Autowired private JobService jobService;
    @Autowired private NotificationService notificationService;
    @Autowired private com.stie.service.AuditService auditService;
    @Autowired private ScorecardService scorecardService;
    @Autowired private UserService userService;
    @Autowired private SalaryRepository salaryRepository;

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List page
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public String listInterviews(Model model) {
        model.addAttribute("pageTitle", "Interview Calendar");
        java.util.List<com.stie.model.Interview> interviews = interviewService.getAllInterviews();
        interviews.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        model.addAttribute("interviews", interviews);
        model.addAttribute("candidates", candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent());
        model.addAttribute("jobs", jobService.getAllVacancies());
        // Load interviewers for the dropdown (scoped to current site)
        com.stie.model.Tenant currentSite = userService.getCurrentSite();
        model.addAttribute("interviewers", currentSite != null
                ? userService.getUsersBySite(currentSite).stream()
                    .filter(u -> u.getRole() != null && u.getRole().contains("INTERVIEWER"))
                    .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList());
        return "interviews";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Schedule — POST (auto-sends invite + .ics)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/schedule")
    public String schedule(@RequestParam Long candidateId,
                           @RequestParam Long jobVacancyId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time,
                           @RequestParam String location,
                           @RequestParam(required = false, defaultValue = "") String interviewMode,
                           @RequestParam(required = false) Long interviewerId,
                           @RequestParam(required = false) String returnUrl,
                           RedirectAttributes redirectAttributes) {
        
        String finalLocation = location;
        if (interviewMode != null && !interviewMode.isEmpty()) {
            finalLocation = "[" + interviewMode + "] " + location;
        }

        try {
            interviewService.scheduleInterview(candidateId, jobVacancyId, time, finalLocation, interviewerId, getCurrentUser());
            redirectAttributes.addFlashAttribute("success",
                    "Interview scheduled successfully! Invite sent automatically to candidate" +
                    (interviewerId != null ? " and interviewer" : "") + " with calendar attachment.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to schedule interview: " + e.getMessage());
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/interviews";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HR Outcome panel — Rejected / KIV / Offered
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/outcome")
    public String recordOutcome(@PathVariable Long id,
                                @RequestParam String outcome,
                                @RequestParam(required = false, defaultValue = "") String notes,
                                RedirectAttributes redirectAttributes) {
        interviewService.recordOutcome(id, outcome, notes, getCurrentUser());
        String label = "REJECTED".equalsIgnoreCase(outcome) ? "Rejection email sent"
                     : "KIV".equalsIgnoreCase(outcome)      ? "Candidate retained in KIV pool"
                                                            : "Offer letter dispatched";
        redirectAttributes.addFlashAttribute("success", label + " — candidate status updated in STIE.");
        return "redirect:/interviews";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resend invite
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/resend-invite")
    public String resendInvite(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        interviewService.resendInvite(id);
        redirectAttributes.addFlashAttribute("success", "Interview invite resent with calendar attachment.");
        return "redirect:/interviews";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Manual reminder (existing button in the table)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/reminder")
    public String sendReminder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Interview interview = interviewService.findById(id).orElse(null);
        if (interview != null) {
            notificationService.sendInterviewReminder(interview);
            redirectAttributes.addFlashAttribute("success",
                    "Reminder sent to " + interview.getCandidate().getFullName()
                    + (interview.getInterviewer() != null ? " and " + interview.getInterviewer().getUsername() : "") + "!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Interview not found.");
        }
        return "redirect:/interviews";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy feedback (existing modal — kept for interviewers)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                 @RequestParam Integer technicalScore,
                                 @RequestParam Integer cultureScore,
                                 @RequestParam Integer communicationScore,
                                 @RequestParam String feedback,
                                 @RequestParam String candidateStatus,
                                 RedirectAttributes redirectAttributes) {
        interviewService.updateOutcome(id, Interview.InterviewStatus.COMPLETED, feedback,
                technicalScore, cultureScore, communicationScore);

        Interview interview = interviewService.findById(id).orElse(null);
        if (interview != null && interview.getCandidate() != null) {
            candidateService.updateStatus(interview.getCandidate().getId(),
                    Candidate.CandidateStatus.valueOf(candidateStatus), getCurrentUser());
        }

        auditService.log("INTERVIEW_FEEDBACK", getCurrentUser(), "Interview", id,
                "Technical: " + technicalScore + ", Culture: " + cultureScore);
        redirectAttributes.addFlashAttribute("success",
                "Interview feedback submitted and candidate status updated.");
        return "redirect:/interviews";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scorecard
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/candidate/{candidateId}/scorecard")
    public String showScorecardByCandidate(@PathVariable Long candidateId, 
                                           @RequestParam(required = false) String modal,
                                           RedirectAttributes redirectAttributes) {
        java.util.List<Interview> interviews = interviewService.getInterviewsByCandidate(candidateId);
        if (interviews == null || interviews.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No interview found for this candidate.");
            return "redirect:/candidates/kanban";
        }
        
        // get latest interview
        Interview latest = interviews.get(interviews.size() - 1);
        
        String redirectUrl = "redirect:/interviews/" + latest.getId() + "/scorecard";
        if ("true".equals(modal)) {
            redirectUrl += "?modal=true";
        }
        return redirectUrl;
    }

    @GetMapping("/{id}/scorecard")
    public String showScorecard(@PathVariable Long id, Model model,
                                @RequestParam(required = false) String modal,
                                RedirectAttributes redirectAttributes) {
        Interview interview = interviewService.findById(id).orElse(null);
        if (interview == null) {
            redirectAttributes.addFlashAttribute("error", "Interview not found.");
            return "redirect:/interviews";
        }
        model.addAttribute("pageTitle", "Panel Evaluation Scorecard");
        model.addAttribute("interview", interview);
        
        if ("true".equals(modal)) {
            model.addAttribute("isModal", true);
        } else {
            model.addAttribute("isModal", false);
        }
        
        java.util.List<com.stie.model.InterviewScorecard> scorecards = scorecardService.getScorecardsByInterview(id);
        boolean hasSubmitted = !scorecards.isEmpty() || interview.getStatus() == Interview.InterviewStatus.COMPLETED;
        boolean isAssignedInterviewer = interview.getInterviewer() != null && 
                                        interview.getInterviewer().getUsername().equals(getCurrentUser());
        
        Salary salary = salaryRepository.findByCandidateIdAndInterviewId(interview.getCandidate().getId(), interview.getId()).orElse(new Salary());

        model.addAttribute("existingScorecards", scorecards);
        model.addAttribute("salary", salary);
        model.addAttribute("hasSubmitted", hasSubmitted);
        model.addAttribute("isAssignedInterviewer", isAssignedInterviewer);
        return "interview-scorecard";
    }

    @PostMapping("/{id}/scorecard")
    public String submitScorecard(@PathVariable Long id,
                                  @ModelAttribute InterviewScorecard scorecardData,
                                  @RequestParam(value = "candidateStatus", required = false) String candidateStatus,
                                  @RequestParam(required = false) String modal,
                                  RedirectAttributes redirectAttributes) {

        Interview interview = interviewService.findById(id).orElse(null);
        if (interview == null) {
            redirectAttributes.addFlashAttribute("error", "Interview not found.");
            return "redirect:/interviews";
        }

        if (interview.getInterviewer() == null || !interview.getInterviewer().getUsername().equals(getCurrentUser())) {
            redirectAttributes.addFlashAttribute("error", "Only the assigned interviewer can submit the scorecard.");
            return "redirect:/interviews/" + id + "/scorecard" + ("true".equals(modal) ? "?modal=true" : "");
        }

        // Map data to a new managed entity (since ModelAttribute creates a detached one)
        InterviewScorecard scorecard = new InterviewScorecard();
        scorecard.setInterview(interview);
        scorecard.setSubmitter(getCurrentUser());
        scorecard.setCreatedAt(LocalDateTime.now());
        
        // 9 Criteria
        scorecard.setCommunicationScore(scorecardData.getCommunicationScore());
        scorecard.setCommunicationRemarks(scorecardData.getCommunicationRemarks());
        scorecard.setJobKnowledgeScore(scorecardData.getJobKnowledgeScore());
        scorecard.setJobKnowledgeRemarks(scorecardData.getJobKnowledgeRemarks());
        scorecard.setResponseScore(scorecardData.getResponseScore());
        scorecard.setResponseRemarks(scorecardData.getResponseRemarks());
        scorecard.setAttitudeScore(scorecardData.getAttitudeScore());
        scorecard.setAttitudeRemarks(scorecardData.getAttitudeRemarks());
        scorecard.setInitiativeScore(scorecardData.getInitiativeScore());
        scorecard.setInitiativeRemarks(scorecardData.getInitiativeRemarks());
        scorecard.setPersonalityScore(scorecardData.getPersonalityScore());
        scorecard.setPersonalityRemarks(scorecardData.getPersonalityRemarks());
        scorecard.setLeadershipScore(scorecardData.getLeadershipScore());
        scorecard.setLeadershipRemarks(scorecardData.getLeadershipRemarks());
        scorecard.setTeamworkScore(scorecardData.getTeamworkScore());
        scorecard.setTeamworkRemarks(scorecardData.getTeamworkRemarks());
        scorecard.setAppearanceScore(scorecardData.getAppearanceScore());
        scorecard.setAppearanceRemarks(scorecardData.getAppearanceRemarks());
        
        scorecard.setSignature1(scorecardData.getSignature1());
        scorecard.setSignature2(scorecardData.getSignature2());
        scorecard.setSignature3(scorecardData.getSignature3());
        
        scorecard.setInterviewer1Name(scorecardData.getInterviewer1Name());
        if (scorecard.getInterviewer1Name() == null || scorecard.getInterviewer1Name().trim().isEmpty()) {
            scorecard.setInterviewer1Name(interview.getInterviewer() != null ? interview.getInterviewer().getDisplayName() : getCurrentUser());
        }
        scorecard.setInterviewer2Name(scorecardData.getInterviewer2Name());
        scorecard.setApproverName(scorecardData.getApproverName());
        
        scorecard.setComments(scorecardData.getComments());
        scorecard.setRecommendation(scorecardData.getRecommendation());

        scorecardService.saveScorecard(scorecard);

        // Calculate average and set on Interview for backwards compatibility
        Double avg = scorecard.getAverageScore();
        int avgInt = (int) Math.round(avg);
        
        interviewService.updateOutcome(id, Interview.InterviewStatus.COMPLETED, scorecard.getComments(),
                avgInt, avgInt, avgInt); // using avg for the old 3 scores

        auditService.log("INTERVIEW_SCORECARD_SUBMIT", getCurrentUser(), "Interview", id,
                "Scorecard by " + getCurrentUser() + ", avg: " + scorecard.getAverageScore());
                
        notificationService.notifyHrFeedbackSubmitted(scorecard);        
                
        redirectAttributes.addFlashAttribute("success", "Panel scorecard saved successfully.");
        
        if ("true".equals(modal)) {
            return "redirect:/interviews/" + id + "/scorecard?modal=true";
        }
        return "redirect:/interviews";
    }

    @PostMapping("/{id}/scorecard/{scorecardId}/hr")
    public String updateScorecardHrFields(@PathVariable Long id,
                                          @PathVariable Long scorecardId,
                                          @RequestParam(required = false) String modal,
                                          @RequestParam(required = false) String positionOffer,
                                          @RequestParam(required = false) String commencementDate,
                                          @RequestParam(required = false) String project,
                                          @RequestParam(required = false) String employmentStatus,
                                          @RequestParam(required = false) String salaryOfferBasic,
                                          @RequestParam(required = false) String salaryOfferTransport,
                                          @RequestParam(required = false) String salaryOfferMobile,
                                          @RequestParam(required = false) String salaryOfferOther,
                                          RedirectAttributes redirectAttributes) {
        
        InterviewScorecard scorecard = scorecardService.getScorecardsByInterview(id).stream()
                .filter(s -> s.getId().equals(scorecardId)).findFirst().orElse(null);

        if (scorecard == null) {
            redirectAttributes.addFlashAttribute("error", "Scorecard not found.");
            return "redirect:/interviews/" + id + "/scorecard";
        }

        scorecard.setPositionOffer(positionOffer);
        scorecard.setCommencementDate(commencementDate);
        scorecard.setProject(project);
        scorecard.setEmploymentStatus(employmentStatus);
        
        scorecardService.saveScorecard(scorecard);

        // Handle Salary
        Salary salary = salaryRepository.findByCandidateIdAndInterviewId(
                scorecard.getInterview().getCandidate().getId(), 
                scorecard.getInterview().getId()
        ).orElse(new Salary());
        salary.setCandidate(scorecard.getInterview().getCandidate());
        salary.setInterview(scorecard.getInterview());
        salary.setBasic(salaryOfferBasic);
        salary.setTransport(salaryOfferTransport);
        salary.setMobile(salaryOfferMobile);
        salary.setOther(salaryOfferOther);
        salaryRepository.save(salary);
        
        auditService.log("SCORECARD_HR_UPDATE", getCurrentUser(), "InterviewScorecard", scorecardId, "HR fields updated");
        redirectAttributes.addFlashAttribute("success", "HR details saved successfully.");
        
        String redirectUrl = "redirect:/interviews/" + id + "/scorecard";
        if ("true".equals(modal)) {
            redirectUrl += "?modal=true";
        }
        return redirectUrl;
    }
}



package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.Interview;
import com.stie.model.InterviewScorecard;
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
            interviewService.scheduleInterview(candidateId, jobVacancyId, time, finalLocation, interviewerId);
            auditService.log("INTERVIEW_SCHEDULE", getCurrentUser(), "Interview", candidateId,
                    "Job: " + jobVacancyId + ", Time: " + time);
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
                    Candidate.CandidateStatus.valueOf(candidateStatus));
            auditService.log("CANDIDATE_STATUS", getCurrentUser(), "Candidate",
                    interview.getCandidate().getId(), "Status → " + candidateStatus);
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

    @GetMapping("/{id}/scorecard")
    public String showScorecard(@PathVariable Long id, Model model,
                                RedirectAttributes redirectAttributes) {
        Interview interview = interviewService.findById(id).orElse(null);
        if (interview == null) {
            redirectAttributes.addFlashAttribute("error", "Interview not found.");
            return "redirect:/interviews";
        }
        model.addAttribute("pageTitle", "Panel Evaluation Scorecard");
        model.addAttribute("interview", interview);
        
        java.util.List<com.stie.model.InterviewScorecard> scorecards = scorecardService.getScorecardsByInterview(id);
        boolean hasSubmitted = !scorecards.isEmpty() || interview.getStatus() == Interview.InterviewStatus.COMPLETED;
        boolean isAssignedInterviewer = interview.getInterviewer() != null && 
                                        interview.getInterviewer().getUsername().equals(getCurrentUser());
        
        model.addAttribute("existingScorecards", scorecards);
        model.addAttribute("hasSubmitted", hasSubmitted);
        model.addAttribute("isAssignedInterviewer", isAssignedInterviewer);
        return "interview-scorecard";
    }

    @PostMapping("/{id}/scorecard")
    public String submitScorecard(@PathVariable Long id,
                                  @RequestParam Integer technicalScore,
                                  @RequestParam Integer problemSolvingScore,
                                  @RequestParam Integer communicationScore,
                                  @RequestParam Integer cultureScore,
                                  @RequestParam String comments,
                                  @RequestParam(value = "candidateStatus", required = false) String candidateStatus,
                                  RedirectAttributes redirectAttributes) {

        Interview interview = interviewService.findById(id).orElse(null);
        if (interview == null) {
            redirectAttributes.addFlashAttribute("error", "Interview not found.");
            return "redirect:/interviews";
        }

        if (interview.getInterviewer() == null || !interview.getInterviewer().getUsername().equals(getCurrentUser())) {
            redirectAttributes.addFlashAttribute("error", "Only the assigned interviewer can submit the scorecard.");
            return "redirect:/interviews/" + id + "/scorecard";
        }

        InterviewScorecard scorecard = new InterviewScorecard(interview, technicalScore, problemSolvingScore,
                communicationScore, cultureScore, comments, getCurrentUser());
        scorecardService.saveScorecard(scorecard);

        interviewService.updateOutcome(id, Interview.InterviewStatus.COMPLETED, interview.getFeedback(),
                interview.getTechnicalScore(), interview.getCultureScore(), interview.getCommunicationScore());

        if (candidateStatus != null && !candidateStatus.isEmpty() && interview.getCandidate() != null) {
            candidateService.updateStatus(interview.getCandidate().getId(),
                    Candidate.CandidateStatus.valueOf(candidateStatus));
            if ("REJECTED".equalsIgnoreCase(candidateStatus)) {
                notificationService.addNotification("Rejection dispatched to: " + interview.getCandidate().getFullName(),
                        "/candidates/" + interview.getCandidate().getId());
            } else if ("OFFERED".equalsIgnoreCase(candidateStatus)) {
                notificationService.addNotification("Offer process initiated for: " + interview.getCandidate().getFullName(),
                        "/candidates/" + interview.getCandidate().getId());
            }
        }

        auditService.log("INTERVIEW_SCORECARD_SUBMIT", getCurrentUser(), "Interview", id,
                "Scorecard by " + getCurrentUser() + ", avg: " + scorecard.getAverageScore());
                
        notificationService.notifyHrFeedbackSubmitted(scorecard);        
                
        redirectAttributes.addFlashAttribute("success", "Panel scorecard saved successfully.");
        return "redirect:/interviews";
    }
}



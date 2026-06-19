package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.service.CandidateService;
import com.stie.service.InterviewService;
import com.stie.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private JobService jobService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private com.stie.service.UserService userService;

    @GetMapping("/")
    public String dashboard(@org.springframework.web.bind.annotation.RequestParam(value = "error", required = false) String error, Model model) {
        com.stie.model.User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        String role = user.getRole(); // returns first role name from Set
        boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
        boolean isInterviewer = user.getRoles().stream().anyMatch(r -> "ROLE_INTERVIEWER".equals(r.getName()));

        if (isSuperAdmin) {
            return "redirect:/super-admin" + (error != null ? "?error=" + error : "");
        } else if ("ROLE_TENANT_ADMIN".equals(role)) {
            return "redirect:/tenant-admin";
        } else if (isInterviewer && user.getRoles().size() == 1) {
            // Only redirect to interviewer dashboard if ONLY role is interviewer
            return "redirect:/interviewer/dashboard";
        } else if ("ROLE_CANDIDATE".equals(role)) {
            return "redirect:/candidate/portal";
        }
        List<Candidate> allCandidates = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent();
        
        long countApplied = allCandidates.stream().filter(c -> c.getStatus() == Candidate.CandidateStatus.APPLIED).count();
        long countShortlisted = allCandidates.stream().filter(c -> c.getStatus() == Candidate.CandidateStatus.SHORTLISTED).count();
        long countInterview = allCandidates.stream().filter(c -> c.getStatus() == Candidate.CandidateStatus.INTERVIEW).count();
        long countOffered = allCandidates.stream().filter(c -> c.getStatus() == Candidate.CandidateStatus.OFFERED).count();
        long countHired = allCandidates.stream().filter(c -> c.getStatus() == Candidate.CandidateStatus.HIRED).count();
        long countRejected = allCandidates.stream().filter(c -> c.getStatus() == Candidate.CandidateStatus.REJECTED).count();
        long countKiv = allCandidates.stream().filter(c -> c.getStatus() == Candidate.CandidateStatus.KIV).count();
        
        long interviewsToday = interviewService.getAllInterviews().stream()
                .filter(i -> i.getInterviewTime() != null && i.getInterviewTime().toLocalDate().equals(java.time.LocalDate.now()))
                .count();

        model.addAttribute("pageTitle", "Recruitment Overview");
        model.addAttribute("totalJobs", jobService.getAllVacancies().size());
        model.addAttribute("totalCandidates", allCandidates.size());
        model.addAttribute("interviewsToday", interviewsToday);
        model.addAttribute("offersSent", countOffered + countHired);

        if (error != null) {
            model.addAttribute("error", error);
        }

        // Funnel stats
        model.addAttribute("countApplied", countApplied);
        model.addAttribute("countShortlisted", countShortlisted);
        model.addAttribute("countInterview", countInterview);
        model.addAttribute("countOffered", countOffered);
        model.addAttribute("countHired", countHired);
        model.addAttribute("countRejected", countRejected);
        model.addAttribute("countKiv", countKiv);
        
        // Latest candidates list
        int limit = Math.min(allCandidates.size(), 5);
        model.addAttribute("latestCandidates", allCandidates.subList(0, limit));

        // ── Interview Calendar Events ──────────────────────────────────────
        // Serialize SCHEDULED interviews to FullCalendar-compatible JSON.
        List<com.stie.model.Interview> scheduledInterviews = interviewService.getAllInterviews().stream()
                .filter(i -> i.getStatus() == com.stie.model.Interview.InterviewStatus.SCHEDULED
                          && i.getInterviewTime() != null)
                .collect(java.util.stream.Collectors.toList());

        java.util.List<java.util.Map<String, Object>> calendarEvents = scheduledInterviews.stream()
                .map(i -> {
                    java.util.Map<String, Object> evt = new java.util.LinkedHashMap<>();
                    evt.put("id", String.valueOf(i.getId()));

                    String candidateName = (i.getCandidate() != null) ? i.getCandidate().getFullName() : "Unknown";
                    String jobTitle      = (i.getJobVacancy()  != null) ? i.getJobVacancy().getTitle()  : "Interview";
                    String siteName      = (i.getTenant() != null) ? i.getTenant().getName() : "Global";
                    
                    evt.put("title", candidateName + " — " + jobTitle + " (" + siteName + ")");

                    // ISO-8601 date-time string that FullCalendar expects
                    evt.put("start", i.getInterviewTime().toString());
                    // 1-hour end block for display width in time-grid views
                    evt.put("end", i.getInterviewTime().plusHours(1).toString());

                    // Emerald colour scheme matching the app theme
                    evt.put("backgroundColor", "#059669");
                    evt.put("borderColor", "#047857");
                    evt.put("textColor", "#ffffff");

                    // Extra data surfaced in the event tooltip / click popup
                    java.util.Map<String, Object> ext = new java.util.LinkedHashMap<>();
                    ext.put("site",        siteName);
                    ext.put("location",    i.getLocation()    != null ? i.getLocation()                    : "TBD");
                    ext.put("candidateId", i.getCandidate()   != null ? i.getCandidate().getId()            : null);
                    ext.put("interviewer", i.getInterviewer() != null ? i.getInterviewer().getUsername()    : "Unassigned");
                    evt.put("extendedProps", ext);

                    evt.put("url", "/interviews");
                    return evt;
                })
                .collect(java.util.stream.Collectors.toList());

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            model.addAttribute("calendarEventsJson", mapper.writeValueAsString(calendarEvents));
        } catch (Exception ex) {
            model.addAttribute("calendarEventsJson", "[]");
        }
        
        return "dashboard";
    }

    @Autowired
    private com.stie.service.NotificationService notificationService;

    @PostMapping("/notifications/read-all")
    public String readAllNotifications(javax.servlet.http.HttpServletRequest request) {
        notificationService.markAllAsRead();
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/notifications/clear-all")
    public String clearAllNotifications(javax.servlet.http.HttpServletRequest request) {
        notificationService.clearAll();
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/notifications/dismiss/{id}")
    public String dismissNotification(@org.springframework.web.bind.annotation.PathVariable Long id,
                                      javax.servlet.http.HttpServletRequest request) {
        notificationService.dismissNotification(id);
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}


package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.Interview;
import com.stie.model.JobVacancy;
import com.stie.model.User;
import com.stie.repository.CandidateRepository;
import com.stie.repository.InterviewRepository;
import com.stie.repository.JobVacancyRepository;
import com.stie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InterviewService {

    @Autowired
    private InterviewRepository repository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobVacancyRepository jobVacancyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserService userService;

    // ─────────────────────────────────────────────────────────────────────────
    // Query helpers
    // ─────────────────────────────────────────────────────────────────────────

    public List<Interview> getAllInterviews() {
        User user = userService.getCurrentUser();
        if (user != null && "ROLE_INTERVIEWER".equals(user.getRole())) {
            return repository.findByInterviewer(user);
        }
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        if (tenant != null) {
            return repository.findByTenant(tenant);
        }
        return repository.findAll();
    }

    public Optional<Interview> findById(Long id) {
        com.stie.model.Tenant tenant = userService.getCurrentTenant();
        return repository.findById(id)
                .filter(i -> tenant == null || i.getTenant() == null || tenant.equals(i.getTenant()));
    }

    public Optional<Interview> findByIdUnscoped(Long id) {
        return repository.findById(id);
    }

    /**
     * Returns all SCHEDULED interviews whose time falls within [now, windowEnd]
     * and for which a reminder has not yet been sent. Used by the hourly scheduler.
     */
    public List<Interview> findPendingReminders(LocalDateTime now, LocalDateTime windowEnd) {
        return repository.findScheduledWithinWindow(now, windowEnd);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Schedule
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates and persists a new interview, assigns interviewer, updates candidate status,
     * and sends the invite email + .ics automatically.
     */
    public Interview scheduleInterview(Long candidateId, Long jobVacancyId,
                                       LocalDateTime time, String location,
                                       Long interviewerId) {

        // Load full entities (not mocked stubs)
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));
        JobVacancy job = jobVacancyRepository.findById(jobVacancyId)
                .orElseThrow(() -> new IllegalArgumentException("Job vacancy not found: " + jobVacancyId));

        Interview interview = new Interview();
        interview.setCandidate(candidate);
        interview.setJobVacancy(job);
        interview.setInterviewTime(time);
        interview.setLocation(location);
        interview.setTenant(userService.getCurrentTenant());

        // Assign interviewer if provided
        if (interviewerId != null) {
            userRepository.findById(interviewerId).ifPresent(interview::setInterviewer);
        }

        // Persist first so the invite has a valid interview ID
        Interview saved = repository.save(interview);

        // Send invite automatically
        notificationService.sendInterviewInvite(saved);
        saved.setInviteSent(true);
        repository.save(saved);

        // Update candidate pipeline stage
        candidateService.updateStatus(candidateId, Candidate.CandidateStatus.INTERVIEW);

        notificationService.addNotification(
                "Interview scheduled for " + candidate.getFullName() + " — invite sent automatically.",
                "/interviews");

        auditService.log("INTERVIEW_SCHEDULED", "HR_User", "Interview", saved.getId(),
                "Candidate: " + candidateId + ", Job: " + jobVacancyId + ", Time: " + time);

        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reminder (called by scheduler)
    // ─────────────────────────────────────────────────────────────────────────

    public void sendReminderAndMark(Interview interview) {
        notificationService.sendInterviewReminder(interview);
        interview.setReminderSent(true);
        repository.save(interview);
        auditService.log("INTERVIEW_REMINDER_SENT", "SCHEDULER", "Interview", interview.getId(),
                "24h reminder dispatched to " + interview.getCandidate().getEmail());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resend invite
    // ─────────────────────────────────────────────────────────────────────────

    public void resendInvite(Long id) {
        repository.findById(id).ifPresent(i -> {
            notificationService.sendInterviewInvite(i);
            i.setInviteSent(true);
            repository.save(i);
            auditService.log("INTERVIEW_INVITE_RESENT", "HR_User", "Interview", i.getId(),
                    "Invite resent to " + i.getCandidate().getEmail());
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Outcome — HR updates after interview completes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates interview status to COMPLETED and applies the HR-selected outcome
     * (REJECTED / KIV / OFFERED) to both the interview record and the candidate.
     * Automatically triggers the appropriate outreach email.
     */
    public void recordOutcome(Long interviewId, String outcome, String notes, String currentUser) {
        repository.findById(interviewId).ifPresent(i -> {
            i.setStatus(Interview.InterviewStatus.COMPLETED);
            i.setFeedback(notes);
            repository.save(i);

            Candidate candidate = i.getCandidate();
            if (candidate == null) return;

            String jobTitle = i.getJobVacancy() != null ? i.getJobVacancy().getTitle() : "the position";

            switch (outcome.toUpperCase()) {
                case "REJECTED":
                    candidateService.updateStatus(candidate.getId(), Candidate.CandidateStatus.REJECTED);
                    notificationService.sendRejectionEmail(candidate.getEmail(), candidate.getFullName());
                    notificationService.addNotification(
                            "Rejection email sent to " + candidate.getFullName(), "/candidates/" + candidate.getId());
                    break;

                case "KIV":
                    candidateService.updateStatus(candidate.getId(), Candidate.CandidateStatus.KIV);
                    notificationService.sendKivEmail(candidate.getEmail(), candidate.getFullName());
                    notificationService.addNotification(
                            candidate.getFullName() + " retained in KIV talent pool.", "/candidates/" + candidate.getId());
                    break;

                case "OFFERED":
                    candidateService.updateStatus(candidate.getId(), Candidate.CandidateStatus.OFFERED);
                    double salary = candidate.getExpectedSalary() != null ? candidate.getExpectedSalary() : 3000;
                    notificationService.sendOfferLetter(candidate.getEmail(), candidate.getFullName(), jobTitle, salary);
                    notificationService.addNotification(
                            "Offer letter sent to " + candidate.getFullName(), "/candidates/" + candidate.getId());
                    break;

                default:
                    break;
            }

            auditService.log("INTERVIEW_OUTCOME", currentUser, "Interview", interviewId,
                    "Outcome: " + outcome + " | Candidate: " + candidate.getFullName());
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy — used by scorecard and feedback endpoints
    // ─────────────────────────────────────────────────────────────────────────

    public void updateOutcome(Long id, Interview.InterviewStatus status, String feedback,
                              Integer tech, Integer cult, Integer comm) {
        repository.findById(id).ifPresent(i -> {
            i.setStatus(status);
            i.setFeedback(feedback);
            if (tech != null) i.setTechnicalScore(tech);
            if (cult != null) i.setCultureScore(cult);
            if (comm != null) i.setCommunicationScore(comm);
            repository.save(i);
        });
    }
}


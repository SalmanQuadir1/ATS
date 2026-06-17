package com.stie.service;

import com.stie.model.Interview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hourly background job that finds SCHEDULED interviews within the next 24 hours
 * (where a reminder hasn't already been sent) and dispatches reminder emails
 * with .ics calendar attachments to both the candidate and the assigned interviewer.
 *
 * The @EnableScheduling annotation is on AtsApplication (already present).
 */
@Service
public class ReminderSchedulerService {

    @Autowired
    private InterviewService interviewService;

    /**
     * Runs every hour (3_600_000 ms).
     * Checks for interviews in the next 24–25 hours to catch any that were
     * missed if the server was down during a previous hourly cycle.
     */
    @Scheduled(fixedRate = 3_600_000) // every 1 hour
    public void sendPendingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(25); // 24h + 1h safety buffer

        List<Interview> pending = interviewService.findPendingReminders(now, windowEnd);

        if (pending.isEmpty()) {
            System.out.println("[SCHEDULER] Reminder check: no pending reminders at " + now);
            return;
        }

        System.out.println("[SCHEDULER] Sending " + pending.size() + " reminder(s) at " + now);
        for (Interview interview : pending) {
            try {
                interviewService.sendReminderAndMark(interview);
                System.out.println("[SCHEDULER] Reminder sent for interview #" + interview.getId()
                        + " → " + interview.getCandidate().getFullName()
                        + " at " + interview.getInterviewTime());
            } catch (Exception e) {
                System.err.println("[SCHEDULER] Failed to send reminder for interview #"
                        + interview.getId() + ": " + e.getMessage());
            }
        }
    }
}


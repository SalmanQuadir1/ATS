package com.stie.service;

import com.stie.model.Interview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // ─────────────────────────────────────────────────────────────────────────
    // In-app notification bell
    // ─────────────────────────────────────────────────────────────────────────

    public static class AppNotification {
        private Long id;
        private String message;
        private String time;
        private boolean read;
        private String targetUrl;

        public AppNotification(Long id, String message, String time, String targetUrl) {
            this.id = id;
            this.message = message;
            this.time = time;
            this.read = false;
            this.targetUrl = targetUrl;
        }

        public Long getId() { return id; }
        public String getMessage() { return message; }
        public String getTime() { return time; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
        public String getTargetUrl() { return targetUrl; }
    }

    private static final java.util.List<AppNotification> notifications =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final java.util.concurrent.atomic.AtomicLong idGenerator =
            new java.util.concurrent.atomic.AtomicLong(3);

    static {
        notifications.add(new AppNotification(1L, "New S-Pass candidate registered: John Tan", "10m ago", "/candidates/1"));
        notifications.add(new AppNotification(2L, "Manpower request approval needed: Senior Java Developer", "1h ago", "/jobs"));
    }

    public java.util.List<AppNotification> getNotifications() { return notifications; }

    public long getUnreadCount() {
        return notifications.stream().filter(n -> !n.isRead()).count();
    }

    public void addNotification(String message, String targetUrl) {
        long id = idGenerator.getAndIncrement();
        notifications.add(0, new AppNotification(id, message, "Just now", targetUrl));
    }

    public void markAllAsRead() { notifications.forEach(n -> n.setRead(true)); }

    public void dismissNotification(Long id) { notifications.removeIf(n -> n.getId().equals(id)); }

    public void clearAll() { notifications.clear(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Interview lifecycle emails  (with .ics calendar attachment)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends interview invitation + .ics calendar file to the candidate AND the assigned interviewer.
     * Called automatically when an interview is scheduled.
     */
    public void sendInterviewInvite(Interview interview) {
        String candidateName = interview.getCandidate().getFullName();
        String candidateEmail = interview.getCandidate().getEmail();
        String jobTitle = interview.getJobVacancy() != null ? interview.getJobVacancy().getTitle() : "the position";
        String timeStr = formatTime(interview);
        String location = interview.getLocation();
        String ics = buildIcsContent(interview, "Interview Invitation: " + jobTitle + " — " + candidateName);

        // Email to candidate
        String candidateSubject = "Interview Invitation — " + jobTitle;
        String candidateBody = "Dear " + candidateName + ",\n\n"
                + "We are pleased to invite you for an interview for the role of " + jobTitle + ".\n\n"
                + "📅 Date & Time : " + timeStr + "\n"
                + "📍 Location    : " + location + "\n\n"
                + "A calendar invite is attached to this email. Please accept it to add the interview to your calendar.\n\n"
                + "Please reply to confirm your attendance. If you are unable to make it, contact us as soon as possible.\n\n"
                + "Best Regards,\nHR Department";
        sendEmailWithIcs(candidateEmail, candidateSubject, candidateBody, ics,
                "interview-invite-" + interview.getId() + ".ics");

        // Email to interviewer (if assigned)
        if (interview.getInterviewer() != null) {
            String interviewerEmail = interview.getInterviewer().getUsername(); // username = email
            String interviewerName = interview.getInterviewer().getUsername();
            String interviewerSubject = "Interview Scheduled: " + candidateName + " — " + jobTitle;
            String interviewerBody = "Dear " + interviewerName + ",\n\n"
                    + "You have been assigned to interview " + candidateName + " for the role of " + jobTitle + ".\n\n"
                    + "📅 Date & Time : " + timeStr + "\n"
                    + "📍 Location    : " + location + "\n\n"
                    + "A calendar invite is attached. Please review the candidate's profile in the STIE system before the interview.\n\n"
                    + "Best Regards,\nHR Department";
            sendEmailWithIcs(interviewerEmail, interviewerSubject, interviewerBody, ics,
                    "interview-invite-" + interview.getId() + ".ics");
        }

        System.out.println("[CALENDAR SYNC] .ics invite sent to: " + candidateEmail
                + (interview.getInterviewer() != null ? " + " + interview.getInterviewer().getUsername() : "")
                + " at " + timeStr);
    }

    /**
     * Backwards-compatible overload for simple calls (reminder button, legacy code).
     */
    public void sendInterviewInvite(String toEmail, String name, String time, String location) {
        String subject = "Interview Invitation \u2014 STIE";
        String body = "Dear " + name + ",\n\nWe are pleased to invite you for an interview on " + time
                + " at " + location + ".\n\nPlease confirm your availability.\n\nBest Regards,\nHR Department";
        sendEmail(toEmail, subject, body);
        System.out.println("[CALENDAR SYNC] Invite sent to: " + toEmail + " at " + time);
    }

    /**
     * Sends 24h reminder + .ics to both candidate and interviewer.
     * Called by ReminderSchedulerService.
     */
    public void sendInterviewReminder(Interview interview) {
        String candidateName = interview.getCandidate().getFullName();
        String candidateEmail = interview.getCandidate().getEmail();
        String jobTitle = interview.getJobVacancy() != null ? interview.getJobVacancy().getTitle() : "the position";
        String timeStr = formatTime(interview);
        String location = interview.getLocation();
        String ics = buildIcsContent(interview, "REMINDER: Interview — " + jobTitle + " — " + candidateName);

        // Candidate reminder
        String candidateSubject = "⏰ Interview Reminder Tomorrow — " + jobTitle;
        String candidateBody = "Dear " + candidateName + ",\n\n"
                + "This is a friendly reminder that your interview is scheduled for tomorrow.\n\n"
                + "📅 Date & Time : " + timeStr + "\n"
                + "📍 Location    : " + location + "\n\n"
                + "Please ensure you are prepared. If you cannot attend, notify HR immediately.\n\n"
                + "Best Regards,\nHR Department";
        sendEmailWithIcs(candidateEmail, candidateSubject, candidateBody, ics,
                "interview-reminder-" + interview.getId() + ".ics");

        // Interviewer reminder
        if (interview.getInterviewer() != null) {
            String interviewerEmail = interview.getInterviewer().getUsername();
            String interviewerSubject = "⏰ Interview Reminder: " + candidateName + " — Tomorrow";
            String interviewerBody = "Dear Interviewer,\n\n"
                    + "Reminder: You are interviewing " + candidateName + " for " + jobTitle + " tomorrow.\n\n"
                    + "📅 Date & Time : " + timeStr + "\n"
                    + "📍 Location    : " + location + "\n\n"
                    + "Please review the candidate's profile in the STIE before the session.\n\n"
                    + "Best Regards,\nHR Department";
            sendEmailWithIcs(interviewerEmail, interviewerSubject, interviewerBody, ics,
                    "interview-reminder-" + interview.getId() + ".ics");
        }

        System.out.println("[REMINDER SENT] 24h reminder dispatched for interview #" + interview.getId()
                + " — candidate: " + candidateEmail);
    }

    /**
     * Legacy overload kept for backwards compatibility with existing controller calls.
     */
    public void sendInterviewReminder(String candidateEmail, String candidateName,
                                      String interviewerEmail, String interviewerName,
                                      String time, String location) {
        String cSubject = "Interview Reminder \u2014 STIE";
        String cBody = "Dear " + candidateName + ",\n\nReminder: Your interview is on " + time + " at " + location + ".\n\nBest Regards,\nHR Department";
        sendEmail(candidateEmail, cSubject, cBody);

        String iSubject = "Interviewer Reminder: Interview with " + candidateName;
        String iBody = "Dear " + interviewerName + ",\n\nReminder: You interview " + candidateName + " on " + time + " at " + location + ".\n\nBest Regards,\nHR Department";
        sendEmail(interviewerEmail, iSubject, iBody);
        System.out.println("[REMINDER SENT] Legacy reminder sent to " + candidateEmail + " & " + interviewerEmail);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Outcome emails
    // ─────────────────────────────────────────────────────────────────────────

    public void sendAcknowledgment(String toEmail, String name) {
        String subject = "Application Received \u2014 STIE";
        String body = "Dear " + name + ",\n\nThank you for applying. Your application is currently being reviewed by our HR team.\n\nBest Regards,\nHR Department";
        sendEmail(toEmail, subject, body);
    }

    public void sendRejectionEmail(String toEmail, String name) {
        String subject = "Application Status Update \u2014 STIE";
        String body = "Dear " + name + ",\n\n"
                + "Thank you for your time and interest in our company. After careful consideration, we regret to inform you that we will not be moving forward with your application at this time.\n\n"
                + "We appreciate your efforts and wish you the very best in your career journey.\n\n"
                + "Best Regards,\nHR Department";
        sendEmail(toEmail, subject, body);
    }

    public void sendKivEmail(String toEmail, String name) {
        String subject = "Application Status — Retained in Talent Pool";
        String body = "Dear " + name + ",\n\n"
                + "Thank you for attending the interview. While we are not proceeding to an immediate offer, we have been impressed with your profile and have retained your application in our active talent pool (Keep In View — KIV).\n\n"
                + "We will reach out should a suitable opportunity matching your profile arise.\n\n"
                + "Best Regards,\nHR Department";
        sendEmail(toEmail, subject, body);
    }

    public void sendOfferLetter(String toEmail, String name, String jobTitle, double salary) {
        String subject = "Employment Offer \u2014 STIE";
        String body = "Dear " + name + ",\n\n"
                + "We are delighted to extend you an employment offer for the position of " + jobTitle + ".\n\n"
                + "Key Offer Details:\n"
                + "  - Designation      : " + jobTitle + "\n"
                + "  - Monthly Salary   : S$" + String.format("%.0f", salary) + "\n"
                + "  - Annual Leave     : 14 Days\n"
                + "  - Probation Period : 3 Months\n\n"
                + "Please review and confirm your acceptance at your earliest convenience.\n\n"
                + "Best Regards,\nHR Department";
        sendEmail(toEmail, subject, body);
    }

    public void shareCandidateWithHM(String hmEmail, String hmName, String candidateName, String candidateUrl) {
        String subject = "Shortlisted Candidate for Review: " + candidateName;
        String body = "Dear " + hmName + ",\n\nHR has shortlisted " + candidateName + " for your review.\n\nView profile: " + candidateUrl + "\n\nBest Regards,\nHR Department";
        sendEmail(hmEmail, subject, body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // .ics Calendar Builder (RFC 5545)
    // ─────────────────────────────────────────────────────────────────────────

    private String buildIcsContent(Interview interview, String summary) {
        DateTimeFormatter icsFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String start = interview.getInterviewTime().format(icsFormat);
        String end = interview.getInterviewTime().plusHours(1).format(icsFormat);
        String now = java.time.LocalDateTime.now().format(icsFormat);
        String uid = UUID.randomUUID().toString() + "@stie-system.com";
        String location = interview.getLocation() != null ? interview.getLocation() : "TBD";

        return "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//STIE Enterprise//Interview Scheduler//EN\r\n"
                + "CALSCALE:GREGORIAN\r\n"
                + "METHOD:REQUEST\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:" + uid + "\r\n"
                + "DTSTAMP:" + now + "Z\r\n"
                + "DTSTART:" + start + "\r\n"
                + "DTEND:" + end + "\r\n"
                + "SUMMARY:" + summary + "\r\n"
                + "LOCATION:" + location + "\r\n"
                + "DESCRIPTION:Interview scheduled via STIE Enterprise System.\r\n"
                + "STATUS:CONFIRMED\r\n"
                + "SEQUENCE:0\r\n"
                + "BEGIN:VALARM\r\n"
                + "TRIGGER:-PT60M\r\n"
                + "ACTION:DISPLAY\r\n"
                + "DESCRIPTION:Interview in 1 hour\r\n"
                + "END:VALARM\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";
    }

    private String formatTime(Interview interview) {
        if (interview.getInterviewTime() == null) return "TBD";
        return interview.getInterviewTime()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy 'at' HH:mm"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core send helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void sendEmailWithIcs(String to, String subject, String body, String icsContent, String icsFilename) {
        if (mailSender != null) {
            try {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body);
                helper.addAttachment(icsFilename,
                        () -> new java.io.ByteArrayInputStream(icsContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        "text/calendar; charset=UTF-8; method=REQUEST");
                mailSender.send(mime);
            } catch (Exception e) {
                System.err.println("[MAIL] Failed to send to " + to + ": " + e.getMessage());
            }
        } else {
            System.out.println("[MOCK EMAIL] To: " + to + " | Subject: " + subject);
            System.out.println("[MOCK .ICS] Calendar event:\n" + icsContent);
        }
    }

    private void sendEmail(String to, String subject, String body) {
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("[MAIL] Failed to send to " + to + ": " + e.getMessage());
            }
        } else {
            System.out.println("[MOCK EMAIL] To: " + to + " | Subject: " + subject);
        }
    }
}



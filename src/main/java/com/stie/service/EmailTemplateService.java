package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.EmailTemplate;
import com.stie.model.JobVacancy;
import com.stie.repository.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
public class EmailTemplateService {

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @PostConstruct
    public void initTemplates() {
        seedTemplatesForTenant(null); // Seed global templates
    }

    public void seedTemplatesForTenant(com.stie.model.Tenant tenant) {
        if (emailTemplateRepository.findByTenant(tenant).isEmpty()) {
            emailTemplateRepository.save(new EmailTemplate(
                "APPLICATION_ACK",
                "Application Acknowledgment",
                "Application Received: {{job_title}} at {{company_name}}",
                "<div style='font-family: sans-serif; padding: 20px; color: #334155;'>" +
                "<h2>Dear {{candidate_name}},</h2>" +
                "<p>Thank you for applying for the <strong>{{job_title}}</strong> position at {{company_name}}!</p>" +
                "<p>We have successfully received your credentials and our recruitment panel is currently evaluating your profile. If your skills match our target requirements, we will contact you for an interview round.</p>" +
                "<br/><p>Warm regards,<br/>Recruitment Team<br/>{{company_name}}</p>" +
                "</div>",
                tenant
            ));

            emailTemplateRepository.save(new EmailTemplate(
                "INTERVIEW_INVITE",
                "Interview Invitation",
                "Interview Scheduled: {{job_title}} at {{company_name}}",
                "<div style='font-family: sans-serif; padding: 20px; color: #334155;'>" +
                "<h2>Dear {{candidate_name}},</h2>" +
                "<p>Great news! We are pleased to invite you for an interview regarding the <strong>{{job_title}}</strong> opening.</p>" +
                "<p><strong>Location:</strong> {{interview_location}}</p>" +
                "<p>Our interviewers are eager to review your background and discuss how your skills align with our goals.</p>" +
                "<br/><p>Best regards,<br/>Talent Acquisition<br/>{{company_name}}</p>" +
                "</div>",
                tenant
            ));

            emailTemplateRepository.save(new EmailTemplate(
                "JOB_OFFER",
                "Job Offer Extended",
                "Official Job Offer: {{job_title}} at {{company_name}}",
                "<div style='font-family: sans-serif; padding: 20px; color: #334155;'>" +
                "<h2>Dear {{candidate_name}},</h2>" +
                "<p>Congratulations! On behalf of {{company_name}}, we are thrilled to offer you the position of <strong>{{job_title}}</strong>.</p>" +
                "<p>We are confident that your expertise and experience will make you a vital contributor to our engineering success.</p>" +
                "<p>Your formal offer letter has been generated with a target salary of <strong>S${{salary}}/month</strong>. Please review, sign, and return it at your earliest convenience.</p>" +
                "<br/><p>Welcome aboard!<br/>Executive Director<br/>{{company_name}}</p>" +
                "</div>",
                tenant
            ));

            emailTemplateRepository.save(new EmailTemplate(
                "REJECTION",
                "Application Status Update",
                "Status Update: {{job_title}} at {{company_name}}",
                "<div style='font-family: sans-serif; padding: 20px; color: #334155;'>" +
                "<h2>Dear {{candidate_name}},</h2>" +
                "<p>Thank you for taking the time to apply and discuss the <strong>{{job_title}}</strong> opening with {{company_name}}.</p>" +
                "<p>While your skills are impressive, we have decided to move forward with another applicant whose technical profiles align more closely with our direct current needs.</p>" +
                "<p>We will keep your resume in our talent pool for future vacancies. We wish you the absolute best in your professional search.</p>" +
                "<br/><p>Sincerely,<br/>Human Resources<br/>{{company_name}}</p>" +
                "</div>",
                tenant
            ));
        }
    }

    public List<EmailTemplate> getAllTemplates(com.stie.model.Tenant tenant) {
        if (tenant != null) {
            return emailTemplateRepository.findByTenant(tenant);
        }
        return emailTemplateRepository.findAll();
    }

    public EmailTemplate getTemplateByCode(String code, com.stie.model.Tenant tenant) {
        if (tenant != null) {
            return emailTemplateRepository.findByTemplateCodeAndTenant(code, tenant)
                    .orElseGet(() -> emailTemplateRepository.findByTemplateCodeAndTenant(code, null).orElse(null));
        }
        return emailTemplateRepository.findByTemplateCodeAndTenant(code, null).orElse(null);
    }

    public void saveTemplate(EmailTemplate template) {
        emailTemplateRepository.save(template);
    }

    /**
     * Parses the template placeholders with actual values
     */
    public String parseTemplate(String body, Candidate candidate, JobVacancy job, String companyName, String extra) {
        if (body == null) return "";
        String parsed = body;
        
        if (candidate != null) {
            parsed = parsed.replace("{{candidate_name}}", candidate.getFullName());
        }
        if (job != null) {
            parsed = parsed.replace("{{job_title}}", job.getTitle());
        }
        parsed = parsed.replace("{{company_name}}", companyName != null ? companyName : "STIE");
        
        if (extra != null) {
            if (extra.contains("location:")) {
                String loc = extra.substring(extra.indexOf("location:") + 9);
                if (loc.contains(";")) loc = loc.split(";")[0];
                parsed = parsed.replace("{{interview_location}}", loc);
            }
            if (extra.contains("salary:")) {
                String sal = extra.substring(extra.indexOf("salary:") + 7);
                if (sal.contains(";")) sal = sal.split(";")[0];
                parsed = parsed.replace("{{salary}}", sal);
            }
        }
        
        return parsed;
    }
}


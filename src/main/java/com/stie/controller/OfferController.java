package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.Interview;
import com.stie.model.InterviewScorecard;
import com.stie.model.JobVacancy;
import com.stie.model.Salary;
import com.stie.repository.SalaryRepository;
import com.stie.service.CandidateService;
import com.stie.service.DocumentService;
import com.stie.service.InterviewService;
import com.stie.service.JobService;
import com.stie.service.ScorecardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/offers")
public class OfferController {

    @Autowired private DocumentService documentService;
    @Autowired private CandidateService candidateService;
    @Autowired private JobService jobService;
    @Autowired private com.stie.service.NotificationService notificationService;
    @Autowired private com.stie.service.AuditService auditService;
    
    @Autowired private InterviewService interviewService;
    @Autowired private ScorecardService scorecardService;
    @Autowired private SalaryRepository salaryRepository;

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private double parseSalaryStr(String val) {
        if (val == null || val.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(val.replaceAll("[^0-9.-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    @GetMapping("/generate/{candidateId}")
    public String showOfferForm(@PathVariable Long candidateId, Model model) {
        Candidate c = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
                .filter(cand -> cand.getId().equals(candidateId)).findFirst().orElse(null);
        
        Interview latestInterview = null;
        InterviewScorecard scorecard = null;
        Salary salary = null;

        if (c != null) {
            java.util.List<Interview> interviews = interviewService.getInterviewsByCandidate(candidateId);
            if (interviews != null && !interviews.isEmpty()) {
                latestInterview = interviews.get(interviews.size() - 1);
                
                java.util.List<InterviewScorecard> scorecards = scorecardService.getScorecardsByInterview(latestInterview.getId());
                if (!scorecards.isEmpty()) {
                    scorecard = scorecards.get(scorecards.size() - 1);
                }
                
                salary = salaryRepository.findByCandidateIdAndInterviewId(candidateId, latestInterview.getId()).orElse(null);
            }
        }

        Double totalSalary = null;
        if (salary != null) {
            double sum = 0;
            sum += parseSalaryStr(salary.getBasic());
            sum += parseSalaryStr(salary.getTransport());
            sum += parseSalaryStr(salary.getMobile());
            sum += parseSalaryStr(salary.getOther());
            if (sum > 0) totalSalary = sum;
        }

        model.addAttribute("candidate", c);
        model.addAttribute("jobs", jobService.getAllVacancies());
        model.addAttribute("latestInterview", latestInterview);
        model.addAttribute("scorecard", scorecard);
        model.addAttribute("salary", salary);
        model.addAttribute("totalSalary", totalSalary);
        model.addAttribute("pageTitle", "Generate Offer Letter");
        return "offer-form";
    }

    @PostMapping("/generate")
    public String generateOffer(@RequestParam Long candidateId, 
                                @RequestParam Long jobVacancyId, 
                                @RequestParam Double salary, 
                                @RequestParam String reportingTo,
                                @RequestParam String commencementDate,
                                @RequestParam String location,
                                @RequestParam String acceptanceDeadline,
                                Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Candidate c = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
                .filter(cand -> cand.getId().equals(candidateId)).findFirst().orElse(null);
        JobVacancy j = jobService.getAllVacancies().stream()
                .filter(job -> job.getId().equals(jobVacancyId)).findFirst().orElse(null);
        
        if (c == null || j == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate or Job not found.");
            return "redirect:/candidates";
        }
        
        String letter = documentService.generateOfferLetter(c, j, salary, reportingTo, commencementDate, location, acceptanceDeadline);
        String pdfFilename = documentService.generateOfferLetterPdf(c, j, salary, reportingTo, commencementDate, location, acceptanceDeadline);
        
        c.setOfferLetterPath(pdfFilename);
        c.setStatus(Candidate.CandidateStatus.OFFERED);
        candidateService.saveCandidate(c);
        
        // Fulfill communication automation by automatically dispatching offer letter
        notificationService.sendOfferLetter(c.getEmail(), c.getFullName(), j.getTitle(), salary, reportingTo, commencementDate, location, acceptanceDeadline, letter);
        auditService.log("OFFER_LETTER_GENERATED", getCurrentUser(), "Candidate", candidateId, 
            "Generated offer of S$" + salary + "/mo for " + j.getTitle() + " and dispatched auto-offer email.");

        model.addAttribute("letter", letter);
        model.addAttribute("pageTitle", "Offer Letter Preview");
        model.addAttribute("success", "Offer letter generated and sent to " + c.getFullName() + " successfully!");
        return "offer-preview";
    }

    /**
     * Renders a standalone printable offer letter page for an existing candidate.
     * The letter is reconstructed from the candidate's stored data.
     */
    @GetMapping("/view/{candidateId}")
    public String viewOfferLetter(@PathVariable Long candidateId, Model model,
                                   org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        Candidate c = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent().stream()
                .filter(cand -> cand.getId().equals(candidateId)).findFirst().orElse(null);
        if (c == null) {
            ra.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/candidates";
        }

        // Reconstruct letter from latest interview / job data
        JobVacancy job = c.getJobVacancy();
        if (job == null && !jobService.getAllVacancies().isEmpty()) {
            job = jobService.getAllVacancies().get(0);
        }
        if (job == null) {
            ra.addFlashAttribute("error", "No job vacancy linked to this candidate.");
            return "redirect:/candidates/" + candidateId;
        }

        // Use stored salary or fall back to expectedSalary
        double salary = c.getFinalSalary() != null ? c.getFinalSalary()
                      : (c.getExpectedSalary() != null ? c.getExpectedSalary() : 0.0);

        String joiningDate = c.getJoiningDate() != null ? c.getJoiningDate().toString() : "TBD";
        String location = job.getLocation() != null ? job.getLocation() : "Singapore";

        String letter = documentService.generateOfferLetter(c, job, salary,
                "HR Manager", joiningDate, location, "TBD");

        model.addAttribute("letter", letter);
        model.addAttribute("candidate", c);
        model.addAttribute("pageTitle", "Offer Letter – " + c.getFullName());
        return "offer-view";
    }
}


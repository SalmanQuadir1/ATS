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

        model.addAttribute("candidate", c);
        model.addAttribute("jobs", jobService.getAllVacancies());
        model.addAttribute("latestInterview", latestInterview);
        model.addAttribute("scorecard", scorecard);
        model.addAttribute("salary", salary);
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
}


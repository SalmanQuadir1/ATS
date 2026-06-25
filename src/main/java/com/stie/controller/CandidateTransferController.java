package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.JobVacancy;
import com.stie.service.CandidateService;
import com.stie.service.CandidateTransferService;
import com.stie.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/transfers")
public class CandidateTransferController {

    @Autowired
    private CandidateTransferService transferService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private JobService jobService;

    @Autowired
    private com.stie.service.UserService userService;

    @GetMapping
    public String listTransfers(Model model) {
        model.addAttribute("pageTitle", "Site Transfers");
        java.util.List<com.stie.model.CandidateTransferRequest> allTransfers = transferService.getAllTransfers();
        java.util.List<com.stie.model.CandidateTransferRequest> activeTransfers = allTransfers.stream()
            .filter(t -> t.getStatus() == com.stie.model.CandidateTransferRequest.TransferStatus.PENDING || 
                         t.getStatus() == com.stie.model.CandidateTransferRequest.TransferStatus.PENDING_DESTINATION)
            .collect(java.util.stream.Collectors.toList());
        java.util.List<com.stie.model.CandidateTransferRequest> historicalTransfers = allTransfers.stream()
            .filter(t -> t.getStatus() == com.stie.model.CandidateTransferRequest.TransferStatus.APPROVED || 
                         t.getStatus() == com.stie.model.CandidateTransferRequest.TransferStatus.REJECTED)
            .collect(java.util.stream.Collectors.toList());

        model.addAttribute("activeTransfers", activeTransfers);
        model.addAttribute("historicalTransfers", historicalTransfers);
        // keep "transfers" just in case
        model.addAttribute("transfers", allTransfers);
        model.addAttribute("candidates", candidateService.getAllCandidates(org.springframework.data.domain.PageRequest.of(0, 10000)).getContent());
        model.addAttribute("jobs", jobService.getAllVacanciesAcrossTenants());
        model.addAttribute("currentTenant", userService.getCurrentTenant());
        return "transfers";
    }

    @PostMapping("/request")
    public String requestTransfer(@RequestParam Long candidateId, 
                                  @RequestParam Long targetJobId, 
                                  @RequestParam(required = false) String notes, 
                                  javax.servlet.http.HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateService.findById(candidateId).orElse(null);
        JobVacancy targetJob = jobService.getJobByIdAcrossTenants(targetJobId);

        if (candidate != null && targetJob != null) {
            if (candidate.getJobVacancy() != null && candidate.getJobVacancy().getId().equals(targetJob.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Candidate is already in the selected job and department.");
            } else {
                transferService.requestTransfer(candidate, targetJob, notes);
                redirectAttributes.addFlashAttribute("successMessage", "Transfer request submitted successfully.");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid candidate or job vacancy.");
        }
        
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/transfers")) {
            return "redirect:/transfers";
        }
        return "redirect:/candidates/" + candidateId;
    }

    @PostMapping("/{id}/approve")
    public String approveTransfer(@PathVariable Long id, 
                                  @RequestParam(required = false) String adminRemarks, 
                                  RedirectAttributes redirectAttributes) {
        transferService.approveTransfer(id, adminRemarks);
        redirectAttributes.addFlashAttribute("successMessage", "Transfer approved successfully.");
        return "redirect:/transfers";
    }

    @PostMapping("/{id}/reject")
    public String rejectTransfer(@PathVariable Long id, 
                                 @RequestParam(required = false) String adminRemarks, 
                                 RedirectAttributes redirectAttributes) {
        transferService.rejectTransfer(id, adminRemarks);
        redirectAttributes.addFlashAttribute("successMessage", "Transfer rejected successfully.");
        return "redirect:/transfers";
    }
}

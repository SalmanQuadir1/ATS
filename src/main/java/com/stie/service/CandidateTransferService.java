package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.CandidateTransferRequest;
import com.stie.model.JobVacancy;
import com.stie.model.Tenant;
import com.stie.repository.CandidateTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateTransferService {

    @Autowired
    private CandidateTransferRepository repository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserService userService;

    @Autowired
    private CandidateService candidateService;

    public List<CandidateTransferRequest> getAllTransfers() {
        Tenant tenant = userService.getCurrentTenant();
        if (tenant != null) {
            return repository.findByTenantOrTargetTenant(tenant, tenant);
        }
        return repository.findAll();
    }

    public List<CandidateTransferRequest> getPendingTransfers() {
        Tenant tenant = userService.getCurrentTenant();
        if (tenant != null) {
            return repository.findByTenantAndStatus(tenant, CandidateTransferRequest.TransferStatus.PENDING);
        }
        return repository.findByStatus(CandidateTransferRequest.TransferStatus.PENDING);
    }

    public CandidateTransferRequest requestTransfer(Candidate candidate, JobVacancy targetJob, String notes) {
        CandidateTransferRequest req = new CandidateTransferRequest();
        req.setCandidate(candidate);
        req.setFromJobVacancy(candidate.getJobVacancy());
        req.setToJobVacancy(targetJob);
        req.setRequestNotes(notes);
        req.setRequestedBy(userService.getCurrentUser() != null ? userService.getCurrentUser().getUsername() : "System");
        req.setTenant(userService.getCurrentTenant());
        req.setTargetTenant(targetJob.getTenant());
        
        CandidateTransferRequest saved = repository.save(req);
        auditService.log("TRANSFER_REQUESTED", req.getRequestedBy(), "CandidateTransferRequest", saved.getId(), "Transfer requested for candidate: " + candidate.getFullName() + " to job: " + targetJob.getTitle());
        return saved;
    }

    @Transactional
    public void approveTransfer(Long transferId, String adminRemarks) {
        Tenant tenant = userService.getCurrentTenant();
        repository.findById(transferId)
            .filter(t -> tenant == null || 
                         (t.getTenant() != null && tenant.getId().equals(t.getTenant().getId())) || 
                         (t.getTargetTenant() != null && tenant.getId().equals(t.getTargetTenant().getId())))
            .ifPresent(t -> {
                boolean isDestAdmin = tenant != null && t.getTargetTenant() != null && tenant.getId().equals(t.getTargetTenant().getId());
                String username = userService.getCurrentUser() != null ? userService.getCurrentUser().getUsername() : "System";

                if (isDestAdmin && t.getStatus() == CandidateTransferRequest.TransferStatus.PENDING) {
                    t.setStatus(CandidateTransferRequest.TransferStatus.APPROVED);
                    t.setDestinationAdminRemarks(adminRemarks);
                    t.setDestinationReviewedBy(username);
                    t.setDestinationReviewedAt(LocalDateTime.now());
                    
                    Candidate c = t.getCandidate();
                    c.setJobVacancy(t.getToJobVacancy());
                    if (t.getToJobVacancy() != null && t.getToJobVacancy().getTenant() != null) {
                        if (t.getTenant() != null && !t.getTenant().getId().equals(t.getToJobVacancy().getTenant().getId())) {
                            c.setTransferSource("Transferred from " + t.getTenant().getName());
                        }
                        c.setTenant(t.getToJobVacancy().getTenant());
                    }
                    candidateService.saveCandidate(c);
                    auditService.log("TRANSFER_APPROVED", username, "CandidateTransferRequest", t.getId(), "Approved transfer for candidate: " + c.getFullName());
                }
                repository.save(t);
            });
    }

    @Transactional
    public void rejectTransfer(Long transferId, String adminRemarks) {
        Tenant tenant = userService.getCurrentTenant();
        repository.findById(transferId)
            .filter(t -> tenant == null || 
                         (t.getTenant() != null && tenant.getId().equals(t.getTenant().getId())) || 
                         (t.getTargetTenant() != null && tenant.getId().equals(t.getTargetTenant().getId())))
            .ifPresent(t -> {
                boolean isDestAdmin = tenant != null && t.getTargetTenant() != null && tenant.getId().equals(t.getTargetTenant().getId());
                String username = userService.getCurrentUser() != null ? userService.getCurrentUser().getUsername() : "System";
                
                if (isDestAdmin && t.getStatus() == CandidateTransferRequest.TransferStatus.PENDING) {
                    t.setStatus(CandidateTransferRequest.TransferStatus.REJECTED);
                    t.setDestinationAdminRemarks(adminRemarks);
                    t.setDestinationReviewedBy(username);
                    t.setDestinationReviewedAt(LocalDateTime.now());
                    auditService.log("TRANSFER_REJECTED", username, "CandidateTransferRequest", t.getId(), "Rejected transfer for candidate: " + t.getCandidate().getFullName());
                }
                repository.save(t);
            });
    }
}

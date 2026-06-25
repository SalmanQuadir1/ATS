package com.stie.repository;

import com.stie.model.CandidateTransferRequest;
import com.stie.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateTransferRepository extends JpaRepository<CandidateTransferRequest, Long> {
    List<CandidateTransferRequest> findByTenant(Tenant tenant);
    List<CandidateTransferRequest> findByTenantOrTargetTenant(Tenant tenant, Tenant targetTenant);
    List<CandidateTransferRequest> findByTenantAndStatus(Tenant tenant, CandidateTransferRequest.TransferStatus status);
    List<CandidateTransferRequest> findByStatus(CandidateTransferRequest.TransferStatus status);
}

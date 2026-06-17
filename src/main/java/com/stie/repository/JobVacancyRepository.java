package com.stie.repository;

import com.stie.model.JobVacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobVacancyRepository extends JpaRepository<JobVacancy, Long> {
    List<JobVacancy> findByApprovalStatus(JobVacancy.ApprovalStatus approvalStatus);
    List<JobVacancy> findByTenant(com.stie.model.Tenant tenant);
    List<JobVacancy> findByTenantAndApprovalStatus(com.stie.model.Tenant tenant, JobVacancy.ApprovalStatus approvalStatus);
    List<JobVacancy> findByHiringManager(com.stie.model.User hiringManager);
}


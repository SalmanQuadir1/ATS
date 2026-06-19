package com.stie.repository;

import com.stie.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    
    List<Candidate> findByFullNameContainingIgnoreCaseOrSkillsContainingIgnoreCase(String name, String skills);
    
    @Query("SELECT c FROM Candidate c WHERE c.experienceYears >= :minExp AND c.nationality = :nat")
    List<Candidate> filterCandidates(@Param("minExp") Integer minExp, @Param("nat") String nationality);

    List<Candidate> findBySecurityLicenseNotNull();
    List<Candidate> findByTenant(com.stie.model.Tenant tenant);
    org.springframework.data.domain.Page<Candidate> findByTenant(com.stie.model.Tenant tenant, org.springframework.data.domain.Pageable pageable);
    java.util.Optional<Candidate> findByCandidateUser(com.stie.model.User user);

    long countByJobVacancy(com.stie.model.JobVacancy jobVacancy);
    long countByJobVacancyAndStatusIn(com.stie.model.JobVacancy jobVacancy, java.util.Collection<Candidate.CandidateStatus> statuses);
    java.util.List<Candidate> findByJobVacancy(com.stie.model.JobVacancy jobVacancy);
}


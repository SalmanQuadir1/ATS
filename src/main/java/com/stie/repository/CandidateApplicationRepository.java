package com.stie.repository;

import com.stie.model.Candidate;
import com.stie.model.CandidateApplication;
import com.stie.model.JobVacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateApplicationRepository extends JpaRepository<CandidateApplication, Long> {

    /** All applications for a specific candidate, newest first */
    List<CandidateApplication> findByCandidateOrderByAppliedAtDesc(Candidate candidate);

    /** All applications linked to a specific job vacancy */
    List<CandidateApplication> findByJobVacancy(JobVacancy jobVacancy);

    /** Count applications for a specific job vacancy */
    long countByJobVacancy(JobVacancy jobVacancy);

    /** Count applications for a specific job vacancy with a given status */
    long countByJobVacancyAndStatus(JobVacancy jobVacancy, CandidateApplication.AppStatus status);

    /** Find all applications for a candidate (unordered) */
    List<CandidateApplication> findByCandidate(Candidate candidate);

    /** Total application count per candidate */
    long countByCandidate(Candidate candidate);

    /** Check if a candidate already applied to a specific job */
    Optional<CandidateApplication> findByCandidateAndJobVacancy(Candidate candidate, JobVacancy jobVacancy);

    /** All applications with a given status */
    List<CandidateApplication> findByStatus(CandidateApplication.AppStatus status);
}

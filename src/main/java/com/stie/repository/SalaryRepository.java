package com.stie.repository;

import com.stie.model.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    Optional<Salary> findByCandidateIdAndInterviewId(Long candidateId, Long interviewId);
    Optional<Salary> findByInterviewId(Long interviewId);
    Optional<Salary> findByCandidateId(Long candidateId);
}

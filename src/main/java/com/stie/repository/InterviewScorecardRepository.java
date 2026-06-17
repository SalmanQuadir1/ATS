package com.stie.repository;

import com.stie.model.InterviewScorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewScorecardRepository extends JpaRepository<InterviewScorecard, Long> {
    List<InterviewScorecard> findByInterviewId(Long interviewId);
    List<InterviewScorecard> findByInterviewCandidateId(Long candidateId);
}


package com.stie.repository;

import com.stie.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByCandidateId(Long candidateId);
    List<Interview> findByTenant(com.stie.model.Tenant tenant);
    List<Interview> findByInterviewer(com.stie.model.User interviewer);

    /** Used by ReminderSchedulerService to find interviews needing a 24h reminder. */
    @Query("SELECT i FROM Interview i WHERE i.status = 'SCHEDULED' " +
           "AND i.reminderSent = false " +
           "AND i.interviewTime BETWEEN :now AND :windowEnd")
    List<Interview> findScheduledWithinWindow(@Param("now") LocalDateTime now,
                                              @Param("windowEnd") LocalDateTime windowEnd);
}



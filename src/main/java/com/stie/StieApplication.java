package com.stie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class StieApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanUpOrphans() {
        System.out.println("Cleaning up orphans...");
        try { jdbcTemplate.update("DELETE FROM interview_scorecards WHERE interview_id IN (SELECT id FROM interviews WHERE candidate_id NOT IN (SELECT id FROM candidates))"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM interviews WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_applications WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM transfer_requests WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_resumes WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_certifications WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_educations WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        System.out.println("Orphan cleanup done.");
    }

	public static void main(String[] args) {
		SpringApplication.run(StieApplication.class, args);
	}

}

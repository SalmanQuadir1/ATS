package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.JobVacancy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DocumentService {

    public String generateOfferLetter(Candidate candidate, JobVacancy job, Double salary) {
        return "OFFER OF EMPLOYMENT\n\n" +
               "Date: " + LocalDate.now() + "\n" +
               "Name: " + candidate.getFullName() + "\n" +
               "Position: " + job.getTitle() + "\n" +
               "Salary: SGD " + salary + " per month\n\n" +
               "We are pleased to offer you the position of " + job.getTitle() + ". " +
               "Please sign and return this document to confirm your acceptance.\n\n" +
               "Regards,\nHR Department";
    }
}


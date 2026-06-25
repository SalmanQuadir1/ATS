package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.JobVacancy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

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

    public String generateOfferLetterPdf(Candidate candidate, JobVacancy job, Double salary) {
        String uploadDir = "uploads/offers/";
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "_Offer_" + candidate.getFullName().replaceAll("\\s+", "_") + ".pdf";
            File file = new File(uploadDir + filename);

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.setLeading(14.5f);
                    contentStream.newLineAtOffset(50, 700);

                    String text = generateOfferLetter(candidate, job, salary);
                    String[] lines = text.split("\n");
                    for (String line : lines) {
                        contentStream.showText(line);
                        contentStream.newLine();
                    }
                    contentStream.endText();
                }

                document.save(file);
            }
            return filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}


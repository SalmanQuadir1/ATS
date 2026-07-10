package com.stie.service;

import com.stie.model.Candidate;
import com.stie.model.JobVacancy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@Service
public class DocumentService {

    public String generateOfferLetter(Candidate candidate, JobVacancy job, Double salary,
                                      String reportingTo, String commencementDate,
                                      String location, String acceptanceDeadline) {
        String companyName = job.getTenant() != null ? job.getTenant().getName() : "STIE Pte Ltd";
        return "Dear " + candidate.getFullName() + ",\n\n" +
               "Congratulations!\n\n" +
               "We are pleased to offer you employment with " + companyName + ". You have been selected for a full-time position of " + job.getTitle() + "\n\n" +
               "We hope that you will enjoy your role and make a significant contribution to the overall success of the Company.\n\n" +
               "Please take the time to review our offer. It includes important details about your compensation & benefits and the terms and conditions of your anticipated employment with " + companyName + ".\n\n" +
               "Position\n" +
               "• Job Title: " + job.getTitle() + "\n" +
               "• Reporting To: " + reportingTo + "\n\n" +
               "Compensation and Benefits\n" +
               "• Monthly Salary: S$ " + String.format("%.2f", salary) + " (inclusive of CPF contributions, where applicable)\n" +
               "• Annual Leave: 14 days per calendar year (pro-rated for an incomplete year of service)\n" +
               "• Hospitalisation Leave and Paid Sick Leave: In accordance with MOM guidelines\n" +
               "• Hospitalisation & Surgical Insurance: Upon successful completion of the probation period - normally three (3 months). Not applicable to contract or freelance employees.\n" +
               "• Outpatient Medical Benefits: Upon successful completion of the probation period - normally three (3 months). Not applicable to contract or freelance employees.\n\n" +
               "Commencement date\n" +
               commencementDate + "\n\n" +
               "Location\n" +
               "You will be based at " + location + " (Head Office or Site).\n\n" +
               "Acceptance of Offer\n" +
               "If you wish to accept this offer, please reply to this email on or before " + acceptanceDeadline + ".\n\n" +
               "Upon your acceptance, HR will prepare your Letter of Appointment (LOA) for signing before your commencement of employment.\n\n" +
               "We look forward to welcoming you to " + companyName + ".\n\n" +
               "If you have any questions or require further information, please feel free to contact me at wpstie@stie.com.sg or phone 91130470.\n\n" +
               "Thank you.\n\n" +
               "Regards,\n" +
               "HR Department\n" +
               companyName;
    }

    public String generateOfferLetterPdf(Candidate candidate, JobVacancy job, Double salary,
                                         String reportingTo, String commencementDate,
                                         String location, String acceptanceDeadline) {
        String uploadDir = com.stie.config.AppConstants.FilePaths.OFFERS_SUBDIR;
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "_Offer_"
                    + candidate.getFullName().replaceAll("\\s+", "_") + ".pdf";
            File file = new File(uploadDir + filename);

            // Margins and layout settings (A4 = 595 x 842 pt)
            float leftMargin   = 65f;
            float rightMargin  = 90f;   // extra right padding
            float topMargin    = 740f;
            float bottomMargin = 60f;
            float fontSize     = 11f;
            float leading      = 16f;
            float pageWidth    = 595f;
            float maxWidth     = pageWidth - leftMargin - rightMargin;

            String text = generateOfferLetter(candidate, job, salary, reportingTo,
                    commencementDate, location, acceptanceDeadline);
            // Sanitize to WinAnsiEncoding (HELVETICA only supports iso-8859-1 range)
            text = sanitizeForPdf(text);
            String[] paragraphs = text.split("\n");

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                PDPageContentStream cs = new PDPageContentStream(document, page);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, fontSize);
                cs.setLeading(leading);
                cs.newLineAtOffset(leftMargin, topMargin);
                float y = topMargin;

                for (String paragraph : paragraphs) {
                    List<String> wrappedLines = wrapText(paragraph, PDType1Font.HELVETICA, fontSize, maxWidth);
                    for (String wl : wrappedLines) {
                        // New page when near the bottom
                        if (y - leading < bottomMargin) {
                            cs.endText();
                            cs.close();
                            page = new PDPage();
                            document.addPage(page);
                            cs = new PDPageContentStream(document, page);
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA, fontSize);
                            cs.setLeading(leading);
                            cs.newLineAtOffset(leftMargin, topMargin);
                            y = topMargin;
                        }
                        cs.showText(wl);
                        cs.newLine();
                        y -= leading;
                    }
                }
                cs.endText();
                cs.close();
                document.save(file);
            }
            return filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Replaces characters outside WinAnsiEncoding with safe ASCII equivalents. */
    private String sanitizeForPdf(String text) {
        return text
            .replace("\u2022", "-")   // bullet -> dash
            .replace("\u2013", "-")   // en-dash
            .replace("\u2014", "-")   // em-dash
            .replace("\u2018", "'")   // left single quote
            .replace("\u2019", "'")   // right single quote
            .replace("\u201C", "\"")  // left double quote
            .replace("\u201D", "\"")  // right double quote
            .replace("\u00A0", " ")   // non-breaking space
            .replaceAll("[^\\x00-\\xFF]", "?"); // anything else outside latin-1
    }

    /** Word-wraps a single line of text so no line exceeds maxWidth points. */
    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            float width;
            try {
                width = font.getStringWidth(test) / 1000 * fontSize;
            } catch (Exception e) {
                // Fallback: estimate 6pt per char
                width = test.length() * 6f;
            }
            if (width > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}

package com.stie.service;

import com.stie.model.Candidate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParserService {

    public Candidate parseResume(MultipartFile file) {
        Candidate candidate = new Candidate();
        String text = "";

        try {
            if (file.getContentType() != null && file.getContentType().equals("application/pdf")) {
                text = extractTextFromPdf(file);
            } else {
                text = new String(file.getBytes());
            }

            if (text == null || text.isEmpty()) return candidate;

            // Regex-based parsing
            String experienceSection = extractSection(text, "experience|work history|employment|career summary");
            String educationSection  = extractSection(text, "education|academic|qualifications");
            String contactSection    = extractSection(text, "contact|personal details|profile");

            candidate.setFullName(extractName(text));
            candidate.setEmail(extractEmail(contactSection.isEmpty() ? text : contactSection));
            candidate.setPhone(extractPhone(contactSection.isEmpty() ? text : contactSection));
            candidate.setSkills(extractSkills(text));
            candidate.setExperienceYears(extractExperienceYears(experienceSection.isEmpty() ? text : experienceSection));
            String eduStr = extractEducation(educationSection.isEmpty() ? text : educationSection);
            if (eduStr != null && !eduStr.isEmpty()) {
                com.stie.model.CandidateEducation ce = new com.stie.model.CandidateEducation();
                ce.setDegree(eduStr);
                candidate.setEducations(java.util.Collections.singletonList(ce));
            }
            candidate.setNationality(extractNationality(text));
            candidate.setSecurityLicense(extractSecurityLicense(text));
            candidate.setPassportNumber(extractPassportNumber(text));
            candidate.setWorkPermitEligible(
                text.toLowerCase().contains("eligible for work permit") ||
                text.toLowerCase().contains(" pr ") ||
                text.toLowerCase().contains("citizen"));

        } catch (Exception e) {
            System.err.println("[ParserService] Error parsing resume: " + e.getMessage());
        }

        return candidate;
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractSection(String text, String keywords) {
        Pattern pattern = Pattern.compile("(?im)^\\s*(" + keywords + ")\\s*\\n+(.*?)(?=\\n\\s*[A-Z][A-Z\\s]{2,30}\\n|\\Z)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return text;
    }

    private String extractName(String text) {
        String[] lines = text.split("\\r?\\n");
        String firstFallback = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase();
            if (lower.contains("resume") || lower.contains("curriculum") || lower.contains("cv") || lower.contains("profile") || lower.contains("page") || lower.contains("email") || lower.contains("phone") ||
                lower.contains("summary") || lower.contains("experience") || lower.contains("history") || lower.contains("employment") || lower.contains("education") || lower.contains("skills") ||
                lower.contains("certification") || lower.contains("project") || lower.contains("objective") || lower.contains("language") || lower.contains("interest") || lower.contains("reference") ||
                lower.contains("contact") || lower.contains("about me") || lower.contains("personal")) continue;

            if (firstFallback == null && trimmed.length() > 2 && trimmed.length() < 50) {
                firstFallback = trimmed;
            }

            String[] words = trimmed.split("\\s+");
            if (words.length >= 2 && words.length <= 5 && trimmed.matches("^[A-Za-z].*")) {
                return trimmed.length() > 250 ? trimmed.substring(0, 250) : trimmed;
            }
        }
        if (firstFallback != null) {
            return firstFallback.length() > 250 ? firstFallback.substring(0, 250) : firstFallback;
        }
        return "Unknown Candidate";
    }

    private String extractEmail(String text) {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private String extractPhone(String text) {
        Pattern patternSg = Pattern.compile("(\\+?65[\\s-]?)?[89]\\d{3}[\\s-]?\\d{4}");
        Matcher matcherSg = patternSg.matcher(text);
        if (matcherSg.find()) return matcherSg.group();

        Pattern patternInt = Pattern.compile("(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}");
        Matcher matcherInt = patternInt.matcher(text);
        if (matcherInt.find()) return matcherInt.group();

        return "";
    }

    private Integer extractExperienceYears(String text) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*(?:\\+|plus)?\\s*(?:years?|yrs?)(?:\\s+of)?\\s+experience", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int maxYears = 0;
        while (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                if (years > maxYears && years < 50) maxYears = years;
            } catch (NumberFormatException e) {}
        }
        if (maxYears > 0) return maxYears;

        pattern = Pattern.compile("(\\d+)\\s*(?:\\+|plus)?\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                if (years > maxYears && years < 50) maxYears = years;
            } catch (NumberFormatException e) {}
        }

        Pattern yearRangePattern = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\s*(?:-|–|—|to)\\s*(19\\d{2}|20\\d{2}|present|current|now)\\b", Pattern.CASE_INSENSITIVE);
        Matcher rangeMatcher = yearRangePattern.matcher(text);
        int rangeYears = 0;
        int currentYear = java.time.Year.now().getValue();
        while (rangeMatcher.find()) {
            try {
                int startYear = Integer.parseInt(rangeMatcher.group(1));
                String endStr = rangeMatcher.group(2).toLowerCase();
                int endYear = currentYear;
                if (!endStr.contains("present") && !endStr.contains("current") && !endStr.contains("now")) {
                    endYear = Integer.parseInt(endStr);
                }
                if (endYear >= startYear && startYear > 1970 && endYear <= currentYear) {
                    rangeYears += (endYear - startYear);
                }
            } catch (NumberFormatException e) {}
        }
        if (rangeYears > 0 && rangeYears < 40) {
            if (rangeYears > maxYears) {
                maxYears = rangeYears;
            }
        }
        return maxYears;
    }

    private String extractEducation(String text) {
        String[] eduKeywords = {
            "Bachelor", "Master", "PhD", "Degree", "Diploma", "B.Sc", "B.A", "M.Sc", "MBA",
            "B.E", "B.Tech", "M.Tech", "B.Com", "M.Com", "Doctorate", "High School", "Polytechnic", "University", "Institute"
        };
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (String kw : eduKeywords) {
                if (line.toLowerCase().contains(kw.toLowerCase())) {
                    String result = line.trim();
                    if (i + 1 < lines.length && !lines[i+1].trim().isEmpty()) {
                        result += " - " + lines[i+1].trim();
                    }
                    return result.length() > 250 ? result.substring(0, 250) : result;
                }
            }
        }
        return "Not Specified";
    }

    private String extractNationality(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("singaporean") ||
            lower.contains("singapore citizen") ||
            lower.contains("citizenship: singapore") ||
            lower.contains("nationality: singapore") ||
            lower.contains("citizen of singapore") ||
            lower.contains("citizenship: sg") ||
            lower.contains("nationality: sg")) {
            return "Singaporean";
        }
        if (lower.contains("permanent resident") ||
            lower.contains("pr") ||
            lower.contains("singapore pr") ||
            lower.contains("singaporepermanentresident")) {
            return "PR";
        }
        return "Foreigner";
    }

    private String extractSecurityLicense(String text) {
        Pattern pattern = Pattern.compile("(?i)(PLRD|Security License|Security ID|SIRD)[:\\s]*([A-Z0-9-]+)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private String extractPassportNumber(String text) {
        Pattern pattern = Pattern.compile("(?i)(Passport|PPT|ID)[:\\s]*([A-Z0-9]{6,12})");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(2) : "";
    }

    private String extractSkills(String text) {
        return ""; // Disabled to only save manually written skills
    }
}


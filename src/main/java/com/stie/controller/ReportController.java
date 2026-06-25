package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.CandidateTransferRequest;
import com.stie.model.Interview;
import com.stie.model.JobVacancy;
import com.stie.model.Tenant;
import com.stie.service.CandidateService;
import com.stie.service.CandidateTransferService;
import com.stie.service.InterviewService;
import com.stie.service.JobService;
import com.stie.service.ReportService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private JobService jobService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private CandidateTransferService transferService;

    @Autowired
    private UserService userService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @GetMapping("/reports")
    public String showReports(Model model) {
        model.addAttribute("pageTitle", "Recruitment Analytics");
        model.addAttribute("avgTimeToFill", 18); // Days
        model.addAttribute("offerAcceptanceRate", 85); // %
        model.addAttribute("totalHires", 24);
        model.addAttribute("avgHiringCost", 1250); // S$
        model.addAttribute("totalCostSpent", 30000); // S$
        return "reports";
    }

    @GetMapping("/reports/candidates/pdf")
    public ResponseEntity<byte[]> downloadCandidatesReport(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            List<Candidate> candidates = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent();
            if (fromDate != null) candidates = candidates.stream().filter(c -> c.getAppliedAt() != null && !c.getAppliedAt().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) candidates = candidates.stream().filter(c -> c.getAppliedAt() != null && !c.getAppliedAt().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();

            for (Candidate c : candidates) {
                Map<String, Object> map = new HashMap<>();
                map.put("candidateName", c.getFullName() != null ? c.getFullName() : "Unknown");
                map.put("email", c.getEmail() != null ? c.getEmail() : "N/A");
                map.put("phone", c.getPhone() != null ? c.getPhone() : "N/A");
                
                String jobTitle = "N/A";
                if (c.getJobVacancy() != null) {
                    jobTitle = c.getJobVacancy().getTitle();
                }
                map.put("jobTitle", jobTitle);
                map.put("status", c.getStatus() != null ? c.getStatus().name() : "N/A");
                
                data.add(map);
            }

            byte[] pdfBytes = reportService.generatePdfReport("candidates_report.jrxml", data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Candidates_Report.pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports/jobs/pdf")
    public ResponseEntity<byte[]> downloadJobsReport(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            List<JobVacancy> jobs = jobService.getAllVacancies();
            if (fromDate != null) jobs = jobs.stream().filter(j -> j.getCreatedAt() != null && !j.getCreatedAt().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) jobs = jobs.stream().filter(j -> j.getCreatedAt() != null && !j.getCreatedAt().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();

            for (JobVacancy j : jobs) {
                Map<String, Object> map = new HashMap<>();
                map.put("title", j.getTitle() != null ? j.getTitle() : "Unknown");
                map.put("department", j.getDepartment() != null ? j.getDepartment() : "N/A");
                map.put("location", j.getLocation() != null ? j.getLocation() : "N/A");
                map.put("status", j.getStatus() != null ? j.getStatus().name() : "N/A");
                map.put("openings", j.getNoOfPosts() != null ? j.getNoOfPosts() : 0);
                
                data.add(map);
            }

            byte[] pdfBytes = reportService.generatePdfReport("jobs_report.jrxml", data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Jobs_Report.pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports/interviews/pdf")
    public ResponseEntity<byte[]> downloadInterviewsReport(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            List<Interview> interviews = interviewService.getAllInterviews();
            if (fromDate != null) interviews = interviews.stream().filter(i -> i.getInterviewTime() != null && !i.getInterviewTime().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) interviews = interviews.stream().filter(i -> i.getInterviewTime() != null && !i.getInterviewTime().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();

            for (Interview i : interviews) {
                Map<String, Object> map = new HashMap<>();
                map.put("candidateName", (i.getCandidate() != null) ? i.getCandidate().getFullName() : "Unknown");
                map.put("interviewerName", (i.getInterviewer() != null) ? i.getInterviewer().getUsername() : "Unassigned");
                map.put("jobTitle", (i.getJobVacancy() != null) ? i.getJobVacancy().getTitle() : "N/A");
                
                String timeStr = "TBD";
                if (i.getInterviewTime() != null) {
                    timeStr = i.getInterviewTime().format(DATETIME_FORMATTER);
                }
                map.put("interviewTime", timeStr);
                map.put("status", i.getStatus() != null ? i.getStatus().name() : "N/A");
                
                data.add(map);
            }

            byte[] pdfBytes = reportService.generatePdfReport("interviews_report.jrxml", data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Interviews_Report.pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports/candidates/excel")
    public ResponseEntity<byte[]> downloadCandidatesExcel(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            List<Candidate> candidates = candidateService.getAllCandidates(PageRequest.of(0, 1000)).getContent();
            if (fromDate != null) candidates = candidates.stream().filter(c -> c.getAppliedAt() != null && !c.getAppliedAt().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) candidates = candidates.stream().filter(c -> c.getAppliedAt() != null && !c.getAppliedAt().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();
            for (Candidate c : candidates) {
                Map<String, Object> map = new HashMap<>();
                map.put("candidateName", c.getFullName() != null ? c.getFullName() : "Unknown");
                map.put("email", c.getEmail() != null ? c.getEmail() : "N/A");
                map.put("phone", c.getPhone() != null ? c.getPhone() : "N/A");
                String jobTitle = "N/A";
                if (c.getJobVacancy() != null) jobTitle = c.getJobVacancy().getTitle();
                map.put("jobTitle", jobTitle);
                map.put("status", c.getStatus() != null ? c.getStatus().name() : "N/A");
                data.add(map);
            }
            byte[] excelBytes = reportService.generateExcelReport("candidates_report.jrxml", data);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("filename", "Candidates_Report.xlsx");
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports/jobs/excel")
    public ResponseEntity<byte[]> downloadJobsExcel(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            List<JobVacancy> jobs = jobService.getAllVacancies();
            if (fromDate != null) jobs = jobs.stream().filter(j -> j.getCreatedAt() != null && !j.getCreatedAt().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) jobs = jobs.stream().filter(j -> j.getCreatedAt() != null && !j.getCreatedAt().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();
            for (JobVacancy j : jobs) {
                Map<String, Object> map = new HashMap<>();
                map.put("title", j.getTitle() != null ? j.getTitle() : "Unknown");
                map.put("department", j.getDepartment() != null ? j.getDepartment() : "N/A");
                map.put("location", j.getLocation() != null ? j.getLocation() : "N/A");
                map.put("status", j.getStatus() != null ? j.getStatus().name() : "N/A");
                map.put("openings", j.getNoOfPosts() != null ? j.getNoOfPosts() : 0);
                data.add(map);
            }
            byte[] excelBytes = reportService.generateExcelReport("jobs_report.jrxml", data);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("filename", "Jobs_Report.xlsx");
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports/interviews/excel")
    public ResponseEntity<byte[]> downloadInterviewsExcel(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            List<Interview> interviews = interviewService.getAllInterviews();
            if (fromDate != null) interviews = interviews.stream().filter(i -> i.getInterviewTime() != null && !i.getInterviewTime().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) interviews = interviews.stream().filter(i -> i.getInterviewTime() != null && !i.getInterviewTime().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();
            for (Interview i : interviews) {
                Map<String, Object> map = new HashMap<>();
                map.put("candidateName", (i.getCandidate() != null) ? i.getCandidate().getFullName() : "Unknown");
                map.put("interviewerName", (i.getInterviewer() != null) ? i.getInterviewer().getUsername() : "Unassigned");
                map.put("jobTitle", (i.getJobVacancy() != null) ? i.getJobVacancy().getTitle() : "N/A");
                String timeStr = "TBD";
                if (i.getInterviewTime() != null) timeStr = i.getInterviewTime().format(DATETIME_FORMATTER);
                map.put("interviewTime", timeStr);
                map.put("status", i.getStatus() != null ? i.getStatus().name() : "N/A");
                data.add(map);
            }
            byte[] excelBytes = reportService.generateExcelReport("interviews_report.jrxml", data);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("filename", "Interviews_Report.xlsx");
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports/transfers/pdf")
    public ResponseEntity<byte[]> downloadTransfersReport(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            Tenant currentTenant = userService.getCurrentTenant();
            List<CandidateTransferRequest> transfers = transferService.getAllTransfers();
            if (fromDate != null) transfers = transfers.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) transfers = transfers.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();

            for (CandidateTransferRequest t : transfers) {
                Map<String, Object> map = new HashMap<>();
                map.put("candidateName", t.getCandidate() != null ? t.getCandidate().getFullName() : "Unknown");
                map.put("fromSite", t.getTenant() != null ? t.getTenant().getName() : "Unknown");
                map.put("toSite", t.getTargetTenant() != null ? t.getTargetTenant().getName() : "Unknown");
                map.put("requestedBy", t.getRequestedBy() != null ? t.getRequestedBy() : "System");
                map.put("status", t.getStatus() != null ? t.getStatus().name() : "N/A");
                
                data.add(map);
            }

            byte[] pdfBytes = reportService.generatePdfReport("transfers_report.jrxml", data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Transfers_Report.pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports/transfers/excel")
    public ResponseEntity<byte[]> downloadTransfersExcel(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        try {
            Tenant currentTenant = userService.getCurrentTenant();
            List<CandidateTransferRequest> transfers = transferService.getAllTransfers();
            if (fromDate != null) transfers = transfers.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isBefore(fromDate)).collect(java.util.stream.Collectors.toList());
            if (toDate != null) transfers = transfers.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isAfter(toDate)).collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> data = new ArrayList<>();

            for (CandidateTransferRequest t : transfers) {
                Map<String, Object> map = new HashMap<>();
                map.put("candidateName", t.getCandidate() != null ? t.getCandidate().getFullName() : "Unknown");
                map.put("fromSite", t.getTenant() != null ? t.getTenant().getName() : "Unknown");
                map.put("toSite", t.getTargetTenant() != null ? t.getTargetTenant().getName() : "Unknown");
                map.put("requestedBy", t.getRequestedBy() != null ? t.getRequestedBy() : "System");
                map.put("status", t.getStatus() != null ? t.getStatus().name() : "N/A");
                
                data.add(map);
            }

            byte[] excelBytes = reportService.generateExcelReport("transfers_report.jrxml", data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("filename", "Transfers_Report.xlsx");
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

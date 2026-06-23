package com.stie.controller;

import com.stie.model.Interview;
import com.stie.model.User;
import com.stie.service.InterviewService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/interviewer")
public class InterviewerController {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        // Strictly displays ONLY interviews assigned to this logged-in interviewer
        List<Interview> assignedInterviews = interviewService.getAllInterviews().stream()
                .filter(i -> i.getInterviewer() != null && i.getInterviewer().getId().equals(user.getId()))
                .collect(Collectors.toList());

        List<Interview> scheduledInterviews = assignedInterviews.stream()
                .filter(i -> i.getStatus() == Interview.InterviewStatus.SCHEDULED)
                .collect(Collectors.toList());

        List<Interview> todayInterviews = assignedInterviews.stream()
                .filter(i -> i.getInterviewTime() != null && i.getInterviewTime().toLocalDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        List<Interview> completedInterviews = assignedInterviews.stream()
                .filter(i -> i.getStatus() == Interview.InterviewStatus.COMPLETED)
                .collect(Collectors.toList());

        model.addAttribute("pageTitle", "My Scheduled Interviews Dashboard");
        model.addAttribute("interviews", assignedInterviews);
        model.addAttribute("scheduledInterviews", scheduledInterviews);
        model.addAttribute("todayInterviews", todayInterviews);
        model.addAttribute("completedInterviews", completedInterviews);
        model.addAttribute("interviewerName", user.getUsername());

        return "interviewer-dashboard";
    }
}


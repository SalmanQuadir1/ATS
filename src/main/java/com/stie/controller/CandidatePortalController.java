package com.stie.controller;

import com.stie.model.Candidate;
import com.stie.model.User;
import com.stie.repository.CandidateRepository;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/candidate/portal")
public class CandidatePortalController {

    @Autowired
    private UserService userService;

    @Autowired
    private CandidateRepository candidateRepository;

    @GetMapping
    public String portal(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        // Get the associated candidate file
        Candidate candidate = candidateRepository.findByCandidateUser(user).orElse(null);

        model.addAttribute("pageTitle", "My Candidate Application Portal");
        model.addAttribute("candidate", candidate);
        model.addAttribute("user", user);

        return "candidate-portal";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String phone,
                                @RequestParam Integer expectedSalary) {
        User user = userService.getCurrentUser();
        if (user != null) {
            candidateRepository.findByCandidateUser(user).ifPresent(c -> {
                c.setPhone(phone);
                c.setExpectedSalary(expectedSalary);
                candidateRepository.save(c);
            });
        }
        return "redirect:/candidate/portal?success=1";
    }
}


package com.stie.controller;
 
import com.stie.service.AuditService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
 
@Controller
public class ProfileController {
 
    @Autowired
    private UserService userService;
 
    @Autowired
    private AuditService auditService;
 
    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
 
    @GetMapping("/profile")
    public String showProfile(Model model) {
        model.addAttribute("pageTitle", "User Profile");
        model.addAttribute("user", userService.findByUsername(getCurrentUser()));
        return "profile";
    }
}


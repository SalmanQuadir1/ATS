package com.stie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Legacy controller — redirects to the new user management page.
 * Kept to avoid 404s on any old bookmarks.
 */
@Controller
@RequestMapping("/tenant-admin")
public class TenantAdminController {

    @GetMapping
    public String index() {
        return "redirect:/users/register";
    }
}


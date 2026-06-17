package com.stie.controller;

import com.stie.model.Location;
import com.stie.model.User;
import com.stie.service.AuditService;
import com.stie.service.LocationService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LocationController {

    @Autowired
    private LocationService locationService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    private String getCurrentUser() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/locations")
    public String list(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            model.addAttribute("error", "Your account does not belong to any Site. SuperAdmin/Global users cannot manage site-specific locations.");
            model.addAttribute("pageTitle", "Locations");
            model.addAttribute("locations", new java.util.ArrayList<>());
            return "locations";
        }
        java.util.List<Location> locs = locationService.getLocationsByTenant(user.getTenant());
        java.util.List<String> oldLocs = user.getTenant().getLocations();
        if (locs.isEmpty() && oldLocs != null && !oldLocs.isEmpty()) {
            for (String name : oldLocs) {
                if (name != null && !name.trim().isEmpty())
                    locationService.create(name.trim(), user.getTenant());
            }
            locs = locationService.getLocationsByTenant(user.getTenant());
        }
        model.addAttribute("pageTitle", "Locations");
        model.addAttribute("locations", locs);
        return "locations";
    }

    @GetMapping("/locations/new")
    public String showCreateForm(Model model) {
        model.addAttribute("pageTitle", "New Location");
        model.addAttribute("location", new Location());
        return "location-form";
    }

    @PostMapping("/locations/create")
    public String create(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (user.getTenant() == null) {
            redirectAttributes.addFlashAttribute("error", "Your account does not belong to any Site. SuperAdmin/Global users cannot manage site-specific locations.");
            return "redirect:/locations";
        }
        Location saved = locationService.create(name, user.getTenant());
        auditService.log("LOCATION_CREATE", getCurrentUser(), "Location", saved.getId(), "Name: " + saved.getName());
        redirectAttributes.addFlashAttribute("success", "Location '" + saved.getName() + "' created.");
        return "redirect:/locations";
    }

    @GetMapping("/locations/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Location loc = locationService.getById(id).orElse(null);
        if (loc == null) return "redirect:/locations";
        model.addAttribute("pageTitle", "Edit Location");
        model.addAttribute("location", loc);
        return "location-form";
    }

    @PostMapping("/locations/{id}/update")
    public String update(@PathVariable Long id, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        Location updated = locationService.update(id, name);
        auditService.log("LOCATION_UPDATE", getCurrentUser(), "Location", id, "Name: " + updated.getName());
        redirectAttributes.addFlashAttribute("success", "Location updated.");
        return "redirect:/locations";
    }

    @PostMapping("/locations/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        locationService.delete(id);
        auditService.log("LOCATION_DELETE", getCurrentUser(), "Location", id, "Deleted");
        redirectAttributes.addFlashAttribute("success", "Location deleted.");
        return "redirect:/locations";
    }
}


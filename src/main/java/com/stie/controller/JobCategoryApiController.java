package com.stie.controller;

import com.stie.model.JobCategory;
import com.stie.model.Skill;
import com.stie.model.User;
import com.stie.service.JobCategoryService;
import com.stie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class JobCategoryApiController {

    @Autowired
    private JobCategoryService categoryService;

    @Autowired
    private UserService userService;

    @GetMapping("/{id}/skills")
    public ResponseEntity<?> getSkillsForCategory(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        if (user == null || user.getTenant() == null) {
            return ResponseEntity.status(403).build();
        }

        Optional<JobCategory> opt = categoryService.getCategoryById(id);
        if (!opt.isPresent() || !opt.get().getTenant().equals(user.getTenant())) {
            return ResponseEntity.status(404).build();
        }

        List<Skill> skills = categoryService.getSkillsByCategory(opt.get());
        List<Map<String, Object>> result = skills.stream()
                .map(s -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", s.getId());
                    map.put("name", s.getName());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}


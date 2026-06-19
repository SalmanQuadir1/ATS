package com.stie.controller;

import com.stie.model.Tenant;
import com.stie.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private TenantRepository tenantRepository;

    @GetMapping("/debug/tenants")
    public List<Tenant> getTenants() {
        return tenantRepository.findAll();
    }

    @GetMapping("/debug/testUrl")
    public String testUrl() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:8080/alpha/apply/1", String.class);
            return "Status: " + response.getStatusCode() + "\nHeaders: " + response.getHeaders() + "\nBody: " + response.getBody();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

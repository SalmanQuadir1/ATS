package com.stie.service;

import com.stie.model.Tenant;
import com.stie.model.User;
import com.stie.repository.TenantRepository;
import com.stie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages Sites (Tenants). Only the SuperAdmin uses this service.
 */
@Service
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private com.stie.repository.RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Tenant> getAllSites() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> getSiteById(Long id) {
        return tenantRepository.findById(id);
    }

    public Tenant getSiteBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain).orElse(null);
    }

    public long getActiveSiteCount() {
        return tenantRepository.findAll().stream().filter(t -> !t.isSuspended()).count();
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    /**
     * Creates a new Site and automatically provisions a Site Admin account.
     */
    public Tenant createSiteWithAdmin(String name, String location, String contactEmail,
                                      String adminUsername, String adminPassword, String adminDisplayName) {
        // Name uniqueness check
        if (tenantRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("A site named '" + name + "' already exists.");
        }
        Tenant site = new Tenant(name, location, contactEmail);
        site.setSubdomain(generateSubdomain(name));
        Tenant saved = tenantRepository.save(site);
        
        emailTemplateService.seedTemplatesForTenant(saved);
        brandingService.getBranding(saved); // This creates default branding if none exists

        if (adminUsername != null && !adminUsername.trim().isEmpty()
                && adminPassword != null && !adminPassword.trim().isEmpty()) {
            if (userRepository.findByUsername(adminUsername.trim()).isPresent()) {
                throw new IllegalArgumentException("Username '" + adminUsername + "' is already taken.");
            }
            User admin = new User(adminUsername.trim(),
                    passwordEncoder.encode(adminPassword.trim()),
                    null,
                    saved,
                    adminDisplayName != null ? adminDisplayName.trim() : adminUsername.trim(),
                    contactEmail);
            com.stie.model.Role adminRole = roleRepository.findByNameAndTenantIsNull("ROLE_ADMIN")
                    .stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
        }
        return saved;
    }

    /**
     * Creates a Site without an admin account.
     */
    public Tenant createSite(String name, String location, String contactEmail) {
        if (tenantRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("A site named '" + name + "' already exists.");
        }
        Tenant site = new Tenant(name, location, contactEmail);
        site.setSubdomain(generateSubdomain(name));
        Tenant saved = tenantRepository.save(site);
        
        emailTemplateService.seedTemplatesForTenant(saved);
        brandingService.getBranding(saved);
        
        return saved;
    }
    
    private String generateSubdomain(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (tenantRepository.findBySubdomain(slug).isPresent()) {
            int counter = 1;
            while (tenantRepository.findBySubdomain(slug + "-" + counter).isPresent()) {
                counter++;
            }
            return slug + "-" + counter;
        }
        return slug;
    }

    public Tenant updateSite(Long id, String name, String location, String contactEmail) {
        Tenant site = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + id));
        site.setName(name);
        site.setLocation(location);
        site.setContactEmail(contactEmail);
        return tenantRepository.save(site);
    }

    public void suspendSite(Long id) {
        tenantRepository.findById(id).ifPresent(t -> {
            t.setSuspended(true);
            tenantRepository.save(t);
        });
    }

    public void resumeSite(Long id) {
        tenantRepository.findById(id).ifPresent(t -> {
            t.setSuspended(false);
            tenantRepository.save(t);
        });
    }

    @Transactional
    public void deleteSite(Long id) {
        jdbcTemplate.update("DELETE FROM candidate_applications WHERE candidate_id IN (SELECT id FROM candidates WHERE tenant_id = ?)", id);
        jdbcTemplate.update("DELETE FROM candidate_transfers WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM interview_scorecards WHERE interview_id IN (SELECT id FROM interviews WHERE tenant_id = ?)", id);
        jdbcTemplate.update("DELETE FROM interviews WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM candidates WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM job_vacancy_skills WHERE job_vacancy_id IN (SELECT id FROM job_vacancies WHERE tenant_id = ?)", id);
        jdbcTemplate.update("DELETE FROM job_vacancies WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM skills WHERE category_id IN (SELECT id FROM job_categories WHERE tenant_id = ?)", id);
        jdbcTemplate.update("DELETE FROM job_categories WHERE tenant_id = ?", id);
        
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE tenant_id = ?)", id);
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE tenant_id = ?)", id);
        jdbcTemplate.update("DELETE FROM roles WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM users WHERE tenant_id = ?", id);
        
        jdbcTemplate.update("DELETE FROM email_templates WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM branding_config WHERE tenant_id = ?", id);
        
        jdbcTemplate.update("DELETE FROM tenant_departments WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM tenant_locations WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM departments WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM locations WHERE tenant_id = ?", id);
        
        tenantRepository.deleteById(id);
    }

    /** Returns users belonging to a site, excluding the SuperAdmin. */
    public List<User> getUsersBySite(Tenant site) {
        return userRepository.findByTenant(site);
    }

    public Tenant updateOrganizationConfig(Long id, List<String> departments, List<String> locations) {
        Tenant site = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + id));
        
        site.getDepartments().clear();
        if (departments != null) {
            for (String d : departments) {
                if (d != null && !d.trim().isEmpty()) site.getDepartments().add(d.trim());
            }
        }
        
        site.getLocations().clear();
        if (locations != null) {
            for (String l : locations) {
                if (l != null && !l.trim().isEmpty()) site.getLocations().add(l.trim());
            }
        }
        
        return tenantRepository.save(site);
    }
}


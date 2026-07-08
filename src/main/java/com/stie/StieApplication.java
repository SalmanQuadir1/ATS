package com.stie;

import com.stie.model.PermissionModule;
import com.stie.repository.PermissionModuleRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class StieApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionModuleRepository permissionModuleRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        cleanUpOrphans();
        seedPermissionModules();
    }

    private void cleanUpOrphans() {
        System.out.println("Cleaning up orphans...");
        try { jdbcTemplate.update("DELETE FROM interview_scorecards WHERE interview_id IN (SELECT id FROM interviews WHERE candidate_id NOT IN (SELECT id FROM candidates))"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM interviews WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_applications WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM transfer_requests WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_resumes WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_certifications WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        try { jdbcTemplate.update("DELETE FROM candidate_educations WHERE candidate_id NOT IN (SELECT id FROM candidates)"); } catch (Exception e) {}
        System.out.println("Orphan cleanup done.");
    }

    /**
     * Seeds all built-in nav modules. Safe to run on every restart —
     * uses findByName to skip already-existing entries.
     */
    private void seedPermissionModules() {
        System.out.println("Seeding permission modules...");

        // name, description, isNavItem, navLabel, navUrl, navIcon, navGroup, navOrder, isSystemModule
        Object[][] modules = {
            // ── Main nav items ──────────────────────────────────────────────
            {"MANAGE_JOBS", "Create, view, and manage job vacancies", true, "Jobs",
             "/jobs",
             "M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z",
             "main", 10, true},

            {"MANAGE_APPLICANTS", "View and manage candidate applications", true, "Job Applications",
             "/candidates/applications",
             "M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z",
             "main", 20, true},

            {"MANAGE_INTERVIEWS", "Schedule and manage interviews", true, "Interviews",
             "/interviews",
             "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2-2v12a2 2 0 002 2z",
             "main", 30, true},

            {"VIEW_CANDIDATES_DB", "Access full candidate database", true, "Candidate Database",
             "/candidates",
             "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z",
             "main", 40, true},

            {"VIEW_PIPELINE", "View pipeline Kanban board", true, "Pipeline Kanban",
             "/candidates/kanban",
             "M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2",
             "main", 50, true},

            {"VIEW_REPORTS", "Access recruitment reports and analytics", true, "Reports",
             "/reports",
             "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z",
             "main", 60, true},

            {"VIEW_HIRED_RECORDS", "View hired and onboarded candidates", true, "Hired Records",
             "/hired-records",
             "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z",
             "main", 70, true},

            // ── Settings nav items ───────────────────────────────────────────
            {"MANAGE_SETTINGS", "Access system settings", false, "Settings",
             "/settings", "", "settings", 10, true},

            {"MANAGE_DEPARTMENTS", "Manage departments and locations", false, "Departments",
             "/departments", "", "settings", 20, true},

            {"MANAGE_ROLES", "Create and manage custom roles", false, "Roles & Permissions",
             "/settings/roles", "", "settings", 30, true},

            // ── Non-nav permissions (backend guards only) ─────────────────
            {"CREATE_JOBS", "Create new job vacancies", false, null, null, null, "main", 0, true},
            {"APPROVE_JOBS", "Approve or reject job vacancy requests", false, null, null, null, "main", 0, true},
        };

        for (Object[] m : modules) {
            String name = (String) m[0];
            if (permissionModuleRepository.findByName(name).isPresent()) continue; // skip if already exists

            PermissionModule pm = new PermissionModule();
            pm.setName(name);
            pm.setDescription((String) m[1]);
            pm.setNavItem((boolean) m[2]);
            pm.setNavLabel((String) m[3]);
            pm.setNavUrl((String) m[4]);
            pm.setNavIcon((String) m[5]);
            pm.setNavGroup((String) m[6]);
            pm.setNavOrder((int) m[7]);
            pm.setSystemModule((boolean) m[8]);
            permissionModuleRepository.save(pm);
        }
        System.out.println("Permission modules seeded.");
    }

	public static void main(String[] args) {
		SpringApplication.run(StieApplication.class, args);
	}

}

package com.stie.config;

import com.stie.model.*;
import com.stie.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private TenantRepository tenantRepo;
    @Autowired private JobVacancyRepository jobRepo;
    @Autowired private CandidateRepository candidateRepo;
    @Autowired private InterviewRepository interviewRepo;
    @Autowired private com.stie.repository.DepartmentRepository departmentRepo;
    @Autowired private com.stie.repository.LocationRepository locationRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private com.stie.repository.RoleRepository roleRepo;
    @Autowired private com.stie.repository.PermissionModuleRepository permissionModuleRepo;
    @Autowired private com.stie.service.EmailTemplateService emailTemplateService;
    @Autowired private com.stie.service.BrandingService brandingService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {

        // ── 0. Fix Constraints (Remove global email uniqueness) ──────────────
        try {
            String constraintName = jdbcTemplate.queryForObject(
                "SELECT CONSTRAINT_NAME FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'candidates' AND constraint_type = 'UNIQUE' AND CONSTRAINT_NAME != 'PRIMARY' LIMIT 1", String.class);
            if (constraintName != null) {
                jdbcTemplate.execute("ALTER TABLE candidates DROP INDEX " + constraintName);
                System.out.println("Dropped global unique constraint on candidates table: " + constraintName);
            }
        } catch (Exception e) {
            // It might not exist or there are no constraints, just continue
        }

        // ── 0. Roles & Permissions (Dynamic RBAC) ──────────────
        String[][] baseModules = {
            {"MANAGE_TENANTS", "Manage all tenants and system settings"},
            {"MANAGE_USERS", "Create, edit, and delete users"},
            {"MANAGE_ROLES", "Create and manage custom roles"},
            {"MANAGE_SETTINGS", "Manage app or tenant settings"},
            {"MANAGE_DEPARTMENTS", "Manage departments and locations"},
            {"CREATE_JOBS", "Create new job vacancies"},
            {"APPROVE_JOBS", "Approve job vacancies for publishing"},
            {"MANAGE_JOBS", "Manage all aspects of jobs (legacy)"},
            {"MANAGE_APPLICANTS", "View and edit job applicants"},
            {"MANAGE_INTERVIEWS", "Schedule and score interviews"},
            {"VIEW_REPORTS", "View analytical reports"},
            {"MANAGE_ONBOARDING", "Manage onboarding tasks"},
            {"MANAGE_OFFERS", "Manage job offers"},
            {"MANAGE_EMPLOYEES", "Manage employee records"},
            {"VIEW_AUDIT_LOGS", "View system audit logs"},
            {"MANAGE_BILLING", "Manage tenant billing"}
        };

        Set<String> allPermissions = new HashSet<>();
        for (String[] mod : baseModules) {
            allPermissions.add(mod[0]);
            if (!permissionModuleRepo.findByName(mod[0]).isPresent()) {
                permissionModuleRepo.save(new PermissionModule(mod[0], mod[1]));
            }
        }

        Role superAdminRole = getOrCreateRole("ROLE_SUPER_ADMIN", allPermissions, true);
        Role adminRole = getOrCreateRole("ROLE_ADMIN", allPermissions, true);
        
        Set<String> hrPermissions = new HashSet<>(allPermissions);
        hrPermissions.remove("CREATE_JOBS"); // HR doesn't create jobs
        Role hrRole = getOrCreateRole("ROLE_HR", hrPermissions, true);
        
        Set<String> managerPermissions = new HashSet<>(allPermissions);
        managerPermissions.remove("APPROVE_JOBS"); // Manager doesn't approve jobs
        Role managerRole = getOrCreateRole("ROLE_MANAGER", managerPermissions, true);
        
        Role interviewerRole = getOrCreateRole("ROLE_INTERVIEWER", allPermissions, true);

        // ── 1. SuperAdmin ────────────────────────────────────────────────────
        User sa = userRepo.findByUsername("superadmin").orElse(null);
        if (sa == null) {
            sa = new User("superadmin", passwordEncoder.encode("admin123"));
            sa.setDisplayName("Super Administrator");
            sa.setEmail("superadmin@stie.com");
            sa.getRoles().add(superAdminRole);
            userRepo.save(sa);
        }

        // Fix existing users that might have lost their roles during migration
        for (User u : userRepo.findAll()) {
            if (u.getRoles().isEmpty()) {
                if (u.getUsername().equals("superadmin")) {
                    u.getRoles().add(superAdminRole);
                } else if (u.getTenant() != null) {
                    // Any site-scoped user without a role gets ROLE_ADMIN (full tenant access)
                    // This is safe: they were previously admins or similar roles
                    if (u.getUsername().startsWith("hr")) u.getRoles().add(hrRole);
                    else if (u.getUsername().startsWith("manager")) u.getRoles().add(managerRole);
                    else if (u.getUsername().startsWith("interviewer")) u.getRoles().add(interviewerRole);
                    else u.getRoles().add(adminRole); // Default: ROLE_ADMIN with full tenant access
                } else {
                    // No tenant and not superadmin — give superadmin role
                    u.getRoles().add(superAdminRole);
                }
                userRepo.save(u);
            }
        }

        // ── 2. Sites ─────────────────────────────────────────────────────────
        com.stie.model.Tenant alpha = tenantRepo.findByName("Alpha Security HQ").orElse(null);
        if (alpha == null) {
            alpha = new com.stie.model.Tenant("Alpha Security HQ",
                    "10 Bayfront Ave, Singapore 018956", "alpha@stie.com");
            alpha.setSubdomain("alpha");
            alpha = tenantRepo.save(alpha);
            
            emailTemplateService.seedTemplatesForTenant(alpha);
            brandingService.getBranding(alpha);
        } else if (alpha.getSubdomain() == null) {
            alpha.setSubdomain("alpha");
            alpha = tenantRepo.save(alpha);
        }

        com.stie.model.Tenant beta = tenantRepo.findByName("Beta Logistics Centre").orElse(null);
        if (beta == null) {
            beta = new com.stie.model.Tenant("Beta Logistics Centre",
                    "10 Airport Blvd, Singapore 819665", "beta@stie.com");
            beta.setSubdomain("beta");
            beta = tenantRepo.save(beta);
            
            emailTemplateService.seedTemplatesForTenant(beta);
            brandingService.getBranding(beta);
        } else if (beta.getSubdomain() == null) {
            beta.setSubdomain("beta");
            beta = tenantRepo.save(beta);
        }

        // Auto-fix any other tenants that might have a null subdomain
        for (com.stie.model.Tenant t : tenantRepo.findAll()) {
            if (t.getSubdomain() == null) {
                String slug = t.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
                if (tenantRepo.findBySubdomain(slug).isPresent()) {
                    int counter = 1;
                    while (tenantRepo.findBySubdomain(slug + "-" + counter).isPresent()) {
                        counter++;
                    }
                    slug = slug + "-" + counter;
                }
                t.setSubdomain(slug);
                tenantRepo.save(t);
            }
        }

        // ── 2b. Seed Departments & Locations ────────────────────────────────
        if (alpha != null && departmentRepo.findByTenantOrderByNameAsc(alpha).isEmpty()) {
            departmentRepo.save(new com.stie.model.Department("Security Operations", alpha));
            departmentRepo.save(new com.stie.model.Department("Human Resources", alpha));
            departmentRepo.save(new com.stie.model.Department("Information Technology", alpha));
            departmentRepo.save(new com.stie.model.Department("Finance", alpha));
        }
        if (alpha != null && locationRepo.findByTenantOrderByNameAsc(alpha).isEmpty()) {
            locationRepo.save(new com.stie.model.Location("10 Bayfront Ave, Singapore", alpha));
            locationRepo.save(new com.stie.model.Location("One Raffles Place, Singapore", alpha));
            locationRepo.save(new com.stie.model.Location("Remote", alpha));
        }
        if (beta != null && departmentRepo.findByTenantOrderByNameAsc(beta).isEmpty()) {
            departmentRepo.save(new com.stie.model.Department("Operations", beta));
            departmentRepo.save(new com.stie.model.Department("Logistics", beta));
            departmentRepo.save(new com.stie.model.Department("Warehouse", beta));
        }
        if (beta != null && locationRepo.findByTenantOrderByNameAsc(beta).isEmpty()) {
            locationRepo.save(new com.stie.model.Location("Changi Airport Terminal 4, Singapore", beta));
            locationRepo.save(new com.stie.model.Location("Pasir Panjang Terminal, Singapore", beta));
            locationRepo.save(new com.stie.model.Location("Remote", beta));
        }

        // ── 3. Alpha Site Users ──────────────────────────────────────────────
        if (!userRepo.findByUsername("siteadmin1").isPresent()) {
            User u = new User("siteadmin1", passwordEncoder.encode("admin123"), null, alpha,
                    "Alice Tan", "alice@alpha.com");
            u.getRoles().add(adminRole);
            userRepo.save(u);
        }
        if (!userRepo.findByUsername("hr1").isPresent()) {
            User u = new User("hr1", passwordEncoder.encode("hr123"), null, alpha,
                    "Bob Lee", "bob@alpha.com");
            u.getRoles().add(hrRole);
            userRepo.save(u);
        }
        User alphaManager = userRepo.findByUsername("manager1").orElse(null);
        if (alphaManager == null) {
            alphaManager = new User("manager1", passwordEncoder.encode("mgr123"), null, alpha,
                    "Carol Ng", "carol@alpha.com");
            alphaManager.getRoles().add(managerRole);
            alphaManager = userRepo.save(alphaManager);
        }
        User alphaInterviewer = userRepo.findByUsername("interviewer1").orElse(null);
        if (alphaInterviewer == null) {
            alphaInterviewer = new User("interviewer1", passwordEncoder.encode("int123"), null, alpha,
                    "David Lim", "david@alpha.com");
            alphaInterviewer.getRoles().add(interviewerRole);
            alphaInterviewer = userRepo.save(alphaInterviewer);
        }

        // ── 4. Beta Site Users ───────────────────────────────────────────────
        if (!userRepo.findByUsername("siteadmin2").isPresent()) {
            User u = new User("siteadmin2", passwordEncoder.encode("admin123"), null, beta,
                    "Eve Sharma", "eve@beta.com");
            u.getRoles().add(adminRole);
            userRepo.save(u);
        }
        User betaHr = userRepo.findByUsername("hr2").orElse(null);
        if (betaHr == null) {
            betaHr = new User("hr2", passwordEncoder.encode("hr123"), null, beta,
                    "Frank Goh", "frank@beta.com");
            betaHr.getRoles().add(hrRole);
            betaHr = userRepo.save(betaHr);
        }
        User betaManager = userRepo.findByUsername("manager2").orElse(null);
        if (betaManager == null) {
            betaManager = new User("manager2", passwordEncoder.encode("mgr123"), null, beta,
                    "Grace Kim", "grace@beta.com");
            betaManager.getRoles().add(managerRole);
            betaManager = userRepo.save(betaManager);
        }

        // ── 5. Alpha Job Vacancies ───────────────────────────────────────────
        JobVacancy alphaJob = null;
        if (jobRepo.findByTenant(alpha).isEmpty()) {
            alphaJob = new JobVacancy(
                    "Security Officer (Shift A)",
                    "We are looking for a dedicated Security Officer to join our team at the Bayfront premises. "
                  + "Responsibilities include access control, CCTV monitoring, and incident reporting. "
                  + "Candidates must hold a valid PLRD license.",
                    "Security Operations");
            alphaJob.setTenant(alpha);
            alphaJob.setLocation("10 Bayfront Ave, Singapore");
            alphaJob.setStatus(JobVacancy.JobStatus.OPEN);
            alphaJob.setApprovalStatus(JobVacancy.ApprovalStatus.APPROVED);
            alphaJob.setHiringManager(alphaManager);
            alphaJob.setNoOfPosts(5);
            alphaJob.setExpiryDate(LocalDateTime.now().plusMonths(2));
            alphaJob = jobRepo.save(alphaJob);

            JobVacancy alphaJob2 = new JobVacancy(
                    "HR Executive",
                    "Responsible for recruitment coordination, onboarding, and HR administration for the security division.",
                    "Human Resources");
            alphaJob2.setTenant(alpha);
            alphaJob2.setLocation("10 Bayfront Ave, Singapore");
            alphaJob2.setStatus(JobVacancy.JobStatus.DRAFT);
            alphaJob2.setApprovalStatus(JobVacancy.ApprovalStatus.PENDING);
            alphaJob2.setHiringManager(alphaManager);
            alphaJob2.setNoOfPosts(1);
            jobRepo.save(alphaJob2);
        } else {
            alphaJob = jobRepo.findByTenant(alpha).stream()
                    .filter(j -> j.getApprovalStatus() == JobVacancy.ApprovalStatus.APPROVED)
                    .findFirst().orElse(null);
        }

        // ── 6. Beta Job Vacancies ────────────────────────────────────────────
        JobVacancy betaJob = null;
        if (jobRepo.findByTenant(beta).isEmpty()) {
            betaJob = new JobVacancy(
                    "Logistics Coordinator",
                    "Coordinate inbound and outbound freight operations, manage shipping documentation, "
                  + "and liaise with carriers and customs authorities.",
                    "Operations");
            betaJob.setTenant(beta);
            betaJob.setLocation("Changi Airport Terminal 4, Singapore");
            betaJob.setStatus(JobVacancy.JobStatus.OPEN);
            betaJob.setApprovalStatus(JobVacancy.ApprovalStatus.APPROVED);
            betaJob.setHiringManager(betaManager);
            betaJob.setNoOfPosts(3);
            betaJob.setExpiryDate(LocalDateTime.now().plusMonths(1));
            betaJob = jobRepo.save(betaJob);
        } else {
            betaJob = jobRepo.findByTenant(beta).stream()
                    .filter(j -> j.getApprovalStatus() == JobVacancy.ApprovalStatus.APPROVED)
                    .findFirst().orElse(null);
        }

        // ── 7. Candidates ────────────────────────────────────────────────────
        Candidate alphaCand = null;
        if (candidateRepo.findAll().stream().noneMatch(c -> "john.tan@alpha.com".equals(c.getEmail()))) {
            alphaCand = new Candidate();
            alphaCand.setFullName("John Tan");
            alphaCand.setEmail("john.tan@alpha.com");
            alphaCand.setPhone("+65 9123 4567");
            alphaCand.setNationality("Singaporean");
            alphaCand.setExperienceYears(4);
            alphaCand.setSkills("PLRD, Security Officer, CCTV, Access Control, First Aid");
            alphaCand.setEducation("Diploma in Security Management - Temasek Polytechnic");
            alphaCand.setStatus(Candidate.CandidateStatus.INTERVIEW);
            alphaCand.setExpectedSalary(2800);
            alphaCand.setSecurityLicense("PLRD-SG-20456");
            alphaCand.setWorkPermitEligible(true);
            alphaCand.setTenant(alpha);
            alphaCand.setJobVacancy(alphaJob);
            alphaCand.setAppliedAt(LocalDateTime.now().minusDays(5));
            alphaCand = candidateRepo.save(alphaCand);
        } else {
            alphaCand = candidateRepo.findAll().stream().filter(c -> "john.tan@alpha.com".equals(c.getEmail())).findFirst().orElse(null);
            if (alphaCand != null && alphaCand.getJobVacancy() == null) {
                alphaCand.setJobVacancy(alphaJob);
                candidateRepo.save(alphaCand);
            }
        }

        if (candidateRepo.findAll().stream().noneMatch(c -> "maria.g@beta.com".equals(c.getEmail()))) {
            Candidate betaCand = new Candidate();
            betaCand.setFullName("Maria Garcia");
            betaCand.setEmail("maria.g@beta.com");
            betaCand.setPhone("+65 8234 5678");
            betaCand.setNationality("PR");
            betaCand.setExperienceYears(6);
            betaCand.setSkills("Logistics, Supply Chain, SAP, Excel, Customer Service");
            betaCand.setEducation("B.Sc in Supply Chain Management - SIM Global Education");
            betaCand.setStatus(Candidate.CandidateStatus.SHORTLISTED);
            betaCand.setExpectedSalary(4200);
            betaCand.setWorkPermitEligible(true);
            betaCand.setTenant(beta);
            betaCand.setJobVacancy(betaJob);
            betaCand.setAppliedAt(LocalDateTime.now().minusDays(3));
            candidateRepo.save(betaCand);
        } else {
            Candidate betaCand = candidateRepo.findAll().stream().filter(c -> "maria.g@beta.com".equals(c.getEmail())).findFirst().orElse(null);
            if (betaCand != null && betaCand.getJobVacancy() == null) {
                betaCand.setJobVacancy(betaJob);
                candidateRepo.save(betaCand);
            }
        }

        // ── 8. Alpha Interview ───────────────────────────────────────────────
        if (alphaCand != null && alphaJob != null
                && interviewRepo.findByInterviewer(alphaInterviewer).isEmpty()) {
            Interview iv = new Interview();
            iv.setCandidate(alphaCand);
            iv.setJobVacancy(alphaJob);
            iv.setInterviewTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0));
            iv.setLocation("Alpha HQ - Conference Room B");
            iv.setStatus(Interview.InterviewStatus.SCHEDULED);
            iv.setTenant(alpha);
            iv.setInterviewer(alphaInterviewer);
            interviewRepo.save(iv);
        }
    }

    /**
     * Gets the first existing global role by name, or creates it.
     * If there are duplicate roles, removes extras from the database.
     */
    private Role getOrCreateRole(String name, Set<String> permissions, boolean isSystemRole) {
        java.util.List<Role> existing = roleRepo.findByNameAndTenantIsNull(name);
        if (existing.isEmpty()) {
            return roleRepo.save(new Role(name, permissions, isSystemRole));
        }
        Role first = existing.get(0);
        // Update permissions on system roles to ensure they're always current
        first.setPermissions(permissions);
        roleRepo.save(first);
        // Delete any duplicate roles (keep only first)
        for (int i = 1; i < existing.size(); i++) {
            roleRepo.delete(existing.get(i));
        }
        return first;
    }
}

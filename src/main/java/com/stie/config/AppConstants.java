package com.stie.config;

/**
 * Application-wide constants for the ATS (Applicant Tracking System).
 *
 * <p>Organise into nested static classes so callers can import only what they need:
 * <pre>
 *   import static com.stie.config.AppConstants.Roles.*;
 *   import static com.stie.config.AppConstants.FilePaths.*;
 * </pre>
 *
 * <p><strong>Convention:</strong> All fields are {@code public static final}.
 * No instances of this class should ever be created.
 */
public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Constants class – do not instantiate.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Roles
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * System role name constants used in Spring Security expressions,
     * {@code @PreAuthorize} annotations, and database seeding.
     */
    public static final class Roles {
        private Roles() {}

        public static final String SUPER_ADMIN    = "ROLE_SUPER_ADMIN";
        public static final String ADMIN          = "ROLE_ADMIN";
        public static final String TENANT_ADMIN   = "ROLE_TENANT_ADMIN";
        public static final String HR             = "ROLE_HR";
        public static final String MANAGER        = "ROLE_MANAGER";
        public static final String INTERVIEWER    = "ROLE_INTERVIEWER";
        public static final String CANDIDATE      = "ROLE_CANDIDATE";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fine-grained permission strings used in {@code @PreAuthorize} and
     * Spring Security {@code hasAuthority()} checks.
     */
    public static final class Permissions {
        private Permissions() {}

        public static final String MANAGE_ROLES       = "MANAGE_ROLES";
        public static final String MANAGE_INTERVIEWS  = "MANAGE_INTERVIEWS";
        public static final String MANAGE_JOBS        = "MANAGE_JOBS";
        public static final String MANAGE_CANDIDATES  = "MANAGE_CANDIDATES";
        public static final String MANAGE_SETTINGS    = "MANAGE_SETTINGS";
        public static final String VIEW_REPORTS       = "VIEW_REPORTS";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File / Upload Paths
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * File-system paths and related upload configuration.
     * Paths are relative to the application working directory unless noted.
     */
    public static final class FilePaths {
        private FilePaths() {}

        /** Root folder where candidate resumes and photos are stored. */
        public static final String UPLOAD_DIR           = "uploads/";

        /** Sub-folder for candidate resume files inside {@link #UPLOAD_DIR}. */
        public static final String RESUME_SUBDIR        = "uploads/resumes/";

        /** Sub-folder for candidate photo files inside {@link #UPLOAD_DIR}. */
        public static final String PHOTO_SUBDIR         = "uploads/photos/";

        /** Sub-folder for tenant branding/logo assets. */
        public static final String BRANDING_SUBDIR      = "uploads/branding/";

        /** URL prefix served by the static resource handler (MvcConfig). */
        public static final String UPLOAD_URL_PREFIX    = "/uploads/";

        /** Maximum file size for resume uploads (mirrors application.properties). */
        public static final long   MAX_FILE_SIZE_BYTES  = 5L * 1024 * 1024; // 5 MB
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MIME / Content Types
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * MIME type strings used when parsing uploaded files or sending email
     * attachments (e.g. PDF resumes, calendar invites).
     */
    public static final class MimeTypes {
        private MimeTypes() {}

        public static final String PDF          = "application/pdf";
        public static final String DOCX         = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        public static final String DOC          = "application/msword";
        public static final String JPEG         = "image/jpeg";
        public static final String PNG          = "image/png";
        public static final String GIF          = "image/gif";
        public static final String CSV          = "text/csv";
        public static final String CALENDAR_ICS = "text/calendar";
        public static final String OCTET_STREAM = "application/octet-stream";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // URL Routes (redirect targets used across controllers)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Common Spring MVC redirect strings returned from controller methods.
     * Use as: {@code return Routes.REDIRECT_LOGIN;}
     */
    public static final class Routes {
        private Routes() {}

        // ── Auth ──────────────────────────────────────────────────────────
        public static final String REDIRECT_LOGIN            = "redirect:/login";

        // ── Dashboard ─────────────────────────────────────────────────────
        public static final String REDIRECT_DASHBOARD        = "redirect:/dashboard";

        // ── Candidates ────────────────────────────────────────────────────
        public static final String REDIRECT_CANDIDATES       = "redirect:/candidates";

        // ── Jobs ──────────────────────────────────────────────────────────
        public static final String REDIRECT_JOBS             = "redirect:/jobs";

        // ── Settings ──────────────────────────────────────────────────────
        public static final String REDIRECT_SETTINGS_ROLES          = "redirect:/settings/roles";
        public static final String REDIRECT_SETTINGS_BRANDING       = "redirect:/settings/branding";
        public static final String REDIRECT_SETTINGS_EMAIL_TEMPLATES = "redirect:/settings/email-templates";
        public static final String REDIRECT_SETTINGS_ORGANISATION   = "redirect:/settings/organization";

        // ── Super-Admin ───────────────────────────────────────────────────
        public static final String REDIRECT_SUPER_ADMIN      = "redirect:/super-admin";
        public static final String REDIRECT_SUPER_ADMIN_ROLES = "redirect:/super-admin/roles";

        // ── User Management ───────────────────────────────────────────────
        public static final String REDIRECT_USER_REGISTER    = "redirect:/users/register";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View Names (Thymeleaf template identifiers)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Logical Thymeleaf view names returned from controller methods.
     */
    public static final class Views {
        private Views() {}

        public static final String APPLY          = "apply";
        public static final String SUCCESS        = "success";
        public static final String LOGIN          = "login";
        public static final String DASHBOARD      = "dashboard";
        public static final String CANDIDATES     = "candidates";
        public static final String JOBS           = "jobs";
        public static final String WALK_IN        = "walk-in";
        public static final String SETTINGS_ROLES = "settings-roles";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Model / Flash Attribute Keys
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * String keys used with {@code Model.addAttribute()},
     * {@code RedirectAttributes.addFlashAttribute()}, and Thymeleaf templates.
     */
    public static final class ModelAttributes {
        private ModelAttributes() {}

        public static final String SUCCESS = "success";
        public static final String ERROR   = "error";
        public static final String MESSAGE = "message";
        public static final String WARNING = "warning";

        public static final String CANDIDATE  = "candidate";
        public static final String JOB        = "job";
        public static final String TENANT     = "tenant";
        public static final String BRANDING   = "branding";
        public static final String USER       = "user";
        public static final String ROLES      = "roles";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audit Action Codes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Audit event codes logged via {@link com.stie.service.AuditService}.
     * Keep in sync with any reporting or dashboard queries on audit_logs.
     */
    public static final class AuditActions {
        private AuditActions() {}

        // Role management
        public static final String ROLE_CREATE  = "ROLE_CREATE";
        public static final String ROLE_UPDATE  = "ROLE_UPDATE";
        public static final String ROLE_DELETE  = "ROLE_DELETE";

        // Job management
        public static final String JOB_CREATED  = "JOB_CREATED";
        public static final String JOB_APPROVED = "JOB_APPROVED";
        public static final String JOB_REJECTED = "JOB_REJECTED";
        public static final String JOB_CLOSED   = "JOB_CLOSED";

        // Interview management
        public static final String INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";
        public static final String INTERVIEW_COMPLETED = "INTERVIEW_COMPLETED";
        public static final String INTERVIEW_CANCELLED = "INTERVIEW_CANCELLED";

        // Candidate management
        public static final String CANDIDATE_APPLIED      = "CANDIDATE_APPLIED";
        public static final String CANDIDATE_SHORTLISTED  = "CANDIDATE_SHORTLISTED";
        public static final String CANDIDATE_REJECTED     = "CANDIDATE_REJECTED";
        public static final String CANDIDATE_HIRED        = "CANDIDATE_HIRED";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Candidate / Interview Status Outcome Strings
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Outcome strings passed as request parameters for interview result
     * processing (e.g. {@code "REJECTED"}, {@code "OFFERED"}, {@code "KIV"}).
     * These correspond to the {@code Candidate.CandidateStatus} enum values.
     */
    public static final class Outcomes {
        private Outcomes() {}

        public static final String REJECTED = "REJECTED";
        public static final String OFFERED  = "OFFERED";
        public static final String HIRED    = "HIRED";
        public static final String KIV      = "KIV";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pagination Defaults
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Default values for paginated list endpoints.
     */
    public static final class Pagination {
        private Pagination() {}

        public static final int DEFAULT_PAGE      = 0;
        public static final int DEFAULT_PAGE_SIZE = 10;
        public static final int MAX_PAGE_SIZE     = 1000;
    }
}

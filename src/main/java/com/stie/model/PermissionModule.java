package com.stie.model;

import javax.persistence.*;

@Entity
@Table(name = "permission_modules")
public class PermissionModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "MANAGE_JOBS"

    @Column(columnDefinition = "TEXT")
    private String description; // e.g. "Create and approve jobs"

    // ─── Nav / Sidebar metadata ─────────────────────────────────────────────

    /** Whether this module should appear as a sidebar nav item */
    @Column(name = "is_nav_item")
    private Boolean isNavItem = false;

    /** Sidebar display label, e.g. "Jobs" */
    @Column(name = "nav_label")
    private String navLabel;

    /** URL for the sidebar link, e.g. "/jobs" */
    @Column(name = "nav_url")
    private String navUrl;

    /** SVG path 'd' attribute for the sidebar icon */
    @Column(name = "nav_icon", columnDefinition = "TEXT")
    private String navIcon;

    /**
     * Grouping key for sidebar sections:
     * "main" — top-level items
     * "settings" — inside the Settings dropdown
     * "admin" — inside User Management dropdown
     */
    @Column(name = "nav_group")
    private String navGroup = "main";

    /** Integer for ordering nav items within their group */
    @Column(name = "nav_order")
    private Integer navOrder = 0;

    /** Whether this is a built-in system module that cannot be deleted */
    @Column(name = "is_system_module")
    private Boolean isSystemModule = false;

    public PermissionModule() {}

    public PermissionModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isNavItem() { return isNavItem != null && isNavItem; }
    public void setNavItem(boolean navItem) { isNavItem = navItem; }

    public String getNavLabel() { return navLabel; }
    public void setNavLabel(String navLabel) { this.navLabel = navLabel; }

    public String getNavUrl() { return navUrl; }
    public void setNavUrl(String navUrl) { this.navUrl = navUrl; }

    public String getNavIcon() { return navIcon; }
    public void setNavIcon(String navIcon) { this.navIcon = navIcon; }

    public String getNavGroup() { return navGroup; }
    public void setNavGroup(String navGroup) { this.navGroup = navGroup; }

    public int getNavOrder() { return navOrder != null ? navOrder : 0; }
    public void setNavOrder(int navOrder) { this.navOrder = navOrder; }

    public boolean isSystemModule() { return isSystemModule != null && isSystemModule; }
    public void setSystemModule(boolean systemModule) { isSystemModule = systemModule; }
}

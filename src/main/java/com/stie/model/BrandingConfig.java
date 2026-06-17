package com.stie.model;

import javax.persistence.*;

@Entity
@Table(name = "branding_config")
public class BrandingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName = "STIE";

    @Column(nullable = false)
    private String portalHeadline = "Join Our Dynamic Teams";

    @Column(nullable = false)
    private String themeColor = "indigo"; // indigo, emerald, rose, slate, violet, amber

    @Column(columnDefinition = "TEXT")
    private String welcomeMessage = "Explore available job openings and start your recruitment journey today.";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    public BrandingConfig() {}

    public BrandingConfig(String companyName, String portalHeadline, String themeColor, String welcomeMessage, Tenant tenant) {
        this.companyName = companyName;
        this.portalHeadline = portalHeadline;
        this.themeColor = themeColor;
        this.welcomeMessage = welcomeMessage;
        this.tenant = tenant;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getPortalHeadline() { return portalHeadline; }
    public void setPortalHeadline(String portalHeadline) { this.portalHeadline = portalHeadline; }

    public String getThemeColor() { return themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
}


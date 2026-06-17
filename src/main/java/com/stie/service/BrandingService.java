package com.stie.service;

import com.stie.model.BrandingConfig;
import com.stie.repository.BrandingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class BrandingService {

    @Autowired
    private BrandingConfigRepository brandingConfigRepository;

    @PostConstruct
    public void initBranding() {
        // Global fallback branding (for tenant = null)
        if (!brandingConfigRepository.findByTenant(null).isPresent()) {
            brandingConfigRepository.save(new BrandingConfig(
                "STIE Recruiting",
                "Start Your Next Great Adventure With Us",
                "indigo",
                "We are constantly searching for bold developers, creative engineers, and operations champions. Explore our vacant roles below.",
                null
            ));
        }
    }

    public BrandingConfig getBranding(com.stie.model.Tenant tenant) {
        return brandingConfigRepository.findByTenant(tenant).orElseGet(() -> {
            BrandingConfig config = new BrandingConfig(
                tenant != null ? tenant.getName() : "Company",
                "Start Your Next Great Adventure With Us",
                "indigo",
                "We are constantly searching for bold developers, creative engineers, and operations champions.",
                tenant
            );
            return brandingConfigRepository.save(config);
        });
    }

    public void saveBranding(BrandingConfig brandingConfig, com.stie.model.Tenant tenant) {
        BrandingConfig current = getBranding(tenant);
        current.setCompanyName(brandingConfig.getCompanyName());
        current.setPortalHeadline(brandingConfig.getPortalHeadline());
        current.setThemeColor(brandingConfig.getThemeColor());
        current.setWelcomeMessage(brandingConfig.getWelcomeMessage());
        brandingConfigRepository.save(current);
    }
}



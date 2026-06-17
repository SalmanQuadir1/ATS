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
        if (brandingConfigRepository.count() == 0) {
            brandingConfigRepository.save(new BrandingConfig(
                "STIE Recruiting",
                "Start Your Next Great Adventure With Us",
                "indigo",
                "We are constantly searching for bold developers, creative engineers, and operations champions. Explore our vacant roles below."
            ));
        }
    }

    public BrandingConfig getBranding() {
        return brandingConfigRepository.findAll().stream().findFirst().orElseGet(() -> {
            BrandingConfig config = new BrandingConfig();
            return brandingConfigRepository.save(config);
        });
    }

    public void saveBranding(BrandingConfig brandingConfig) {
        BrandingConfig current = getBranding();
        current.setCompanyName(brandingConfig.getCompanyName());
        current.setPortalHeadline(brandingConfig.getPortalHeadline());
        current.setThemeColor(brandingConfig.getThemeColor());
        current.setWelcomeMessage(brandingConfig.getWelcomeMessage());
        brandingConfigRepository.save(current);
    }
}



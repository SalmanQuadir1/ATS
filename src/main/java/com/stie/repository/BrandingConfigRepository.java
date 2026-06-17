package com.stie.repository;

import com.stie.model.BrandingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandingConfigRepository extends JpaRepository<BrandingConfig, Long> {
}


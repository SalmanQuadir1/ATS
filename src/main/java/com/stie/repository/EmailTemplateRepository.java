package com.stie.repository;

import com.stie.model.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByTemplateCodeAndTenant(String templateCode, com.stie.model.Tenant tenant);
    java.util.List<EmailTemplate> findByTenant(com.stie.model.Tenant tenant);
}


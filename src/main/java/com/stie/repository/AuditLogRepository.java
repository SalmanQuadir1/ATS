package com.stie.repository;

import com.stie.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    java.util.List<AuditLog> findByTargetEntityAndTargetIdOrderByTimestampDesc(String targetEntity, Long targetId);
    java.util.List<AuditLog> findByTenantOrderByTimestampDesc(com.stie.model.Tenant tenant);
}


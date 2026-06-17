package com.stie.service;

import com.stie.model.AuditLog;
import com.stie.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository repository;

    public void log(String action, String user, String entity, Long id, String details) {
        log(action, user, entity, id, details, null);
    }

    public void log(String action, String user, String entity, Long id, String details, com.stie.model.Tenant tenant) {
        AuditLog log = new AuditLog(action, user, entity, id, details, tenant);
        repository.save(log);
    }

    public List<AuditLog> getRecentLogs(com.stie.model.Tenant tenant) {
        if (tenant != null) {
            return repository.findByTenantOrderByTimestampDesc(tenant);
        }
        return repository.findAll(); // For SuperAdmin
    }

    public List<AuditLog> getLogsForEntity(String entity, Long id) {
        return repository.findByTargetEntityAndTargetIdOrderByTimestampDesc(entity, id);
    }
}


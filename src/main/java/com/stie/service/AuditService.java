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
        AuditLog log = new AuditLog(action, user, entity, id, details);
        repository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return repository.findAll(); // In production: findTop50ByOrderByTimestampDesc()
    }

    public List<AuditLog> getLogsForEntity(String entity, Long id) {
        return repository.findByTargetEntityAndTargetIdOrderByTimestampDesc(entity, id);
    }
}


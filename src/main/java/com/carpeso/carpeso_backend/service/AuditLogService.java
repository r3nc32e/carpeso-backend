package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.model.AuditLog;
import com.carpeso.carpeso_backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String action, String performedBy,
                    String targetEntity, String targetId,
                    String details, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByUser(String email) {
        return auditLogRepository.findByPerformedByOrderByTimestampDesc(email);
    }

    public List<AuditLog> getLogsByEntity(String entity) {
        return auditLogRepository.findByTargetEntityOrderByTimestampDesc(entity);
    }
}
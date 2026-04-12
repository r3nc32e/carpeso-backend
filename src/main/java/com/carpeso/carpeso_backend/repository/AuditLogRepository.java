package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy);
    List<AuditLog> findByTargetEntityOrderByTimestampDesc(String targetEntity);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
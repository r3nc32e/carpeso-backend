package com.carpeso.carpeso_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    private String performedBy;
    private String targetEntity;
    private String targetId;

    @Column(length = 2000)
    private String details;

    private String ipAddress;
    private String userAgent;
    private String sessionId;

    private LocalDateTime timestamp = LocalDateTime.now();
}
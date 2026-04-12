package com.carpeso.carpeso_backend.model;

import com.carpeso.carpeso_backend.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead = false;
    private String link;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;
}
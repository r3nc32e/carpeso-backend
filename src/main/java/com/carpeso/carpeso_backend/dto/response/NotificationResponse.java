package com.carpeso.carpeso_backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private String link;
    private LocalDateTime createdAt;
}
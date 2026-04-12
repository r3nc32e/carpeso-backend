package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.service.AuthService;
import com.carpeso.carpeso_backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<?> getMyNotifications(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    "Notifications fetched!",
                    notificationService.getUserNotifications(user.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            long count = notificationService.getUnreadCount(user.getId());
            return ResponseEntity.ok(
                    ApiResponse.success("Count fetched!", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read!"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            notificationService.markAllAsRead(user.getId());
            return ResponseEntity.ok(
                    ApiResponse.success("All marked as read!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
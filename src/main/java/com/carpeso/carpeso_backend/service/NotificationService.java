package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.model.Notification;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.enums.NotificationType;
import com.carpeso.carpeso_backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void send(User user, String title, String message,
                     NotificationType type, String link) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(type);
        notif.setLink(link);
        notif.setExpiresAt(LocalDateTime.now().plusDays(7));
        notificationRepository.save(notif);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
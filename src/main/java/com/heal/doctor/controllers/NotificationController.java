package com.heal.doctor.controllers;

import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.services.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getAllNotifications() {
        List<NotificationResponseDTO> notifications = notificationService.getAllNotificationsForCurrentDoctor();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications() {
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotificationsForCurrentDoctor();
        return ResponseEntity.ok(notifications);
    }

//    @PostMapping
//    public ResponseEntity<NotificationResponseDTO> createNotification(@RequestBody NotificationEntity request) {
//        NotificationResponseDTO created = notificationService.createNotification(request);
//        return ResponseEntity.ok(created);
//    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable String id) {
        NotificationResponseDTO updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsReadForCurrentDoctor();
        return ResponseEntity.noContent().build();
    }
}

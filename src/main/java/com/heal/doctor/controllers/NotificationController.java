package com.heal.doctor.controllers;

import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.utils.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getAllNotifications() {
        List<NotificationResponseDTO> notifications = notificationService.getAllNotificationsForCurrentDoctor();
        ApiResponse<List<NotificationResponseDTO>> response = ApiResponse.<List<NotificationResponseDTO>>builder()
                .success(true)
                .message("Fetched all notifications successfully.")
                .data(notifications)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getUnreadNotifications() {
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotificationsForCurrentDoctor();
        ApiResponse<List<NotificationResponseDTO>> response = ApiResponse.<List<NotificationResponseDTO>>builder()
                .success(true)
                .message("Fetched unread notifications successfully.")
                .data(notifications)
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> markAsRead(@PathVariable String id) {
        NotificationResponseDTO updated = notificationService.markAsRead(id);
        ApiResponse<NotificationResponseDTO> response = ApiResponse.<NotificationResponseDTO>builder()
                .success(true)
                .message("Notification marked as read successfully.")
                .data(updated)
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> markAllAsRead() {
        List<NotificationResponseDTO> notifications= notificationService.markAllAsReadForCurrentDoctor();
        ApiResponse<List<NotificationResponseDTO>> response = ApiResponse.<List<NotificationResponseDTO>>builder()
                .success(true)
                .message("All notifications marked as read successfully.")
                .data(notifications)
                .build();
        return ResponseEntity.ok(response);
    }
}

package com.heal.doctor.services;

import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.models.NotificationEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface INotificationService {
    NotificationResponseDTO createNotification(NotificationEntity notification);

    CompletableFuture<Void> createNotificationAsync(NotificationEntity notification);

    List<NotificationResponseDTO> getAllNotificationsForCurrentDoctor();

    List<NotificationResponseDTO> getUnreadNotificationsForCurrentDoctor();

    NotificationResponseDTO markAsRead(String notificationId);

    List<NotificationResponseDTO> markAllAsReadForCurrentDoctor();
}

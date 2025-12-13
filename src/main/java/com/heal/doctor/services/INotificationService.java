package com.heal.doctor.services;

import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface INotificationService {
    NotificationResponseDTO createNotification(NotificationEntity notification);

    CompletableFuture<Void> createNotificationAsync(NotificationEntity notification);

    List<NotificationResponseDTO> getAllNotificationsForCurrentDoctor();

    List<NotificationResponseDTO> getUnreadNotificationsForCurrentDoctor();

    NotificationResponseDTO markAsRead(String notificationId);

    List<NotificationResponseDTO> markAllAsReadForCurrentDoctor();

    /**
     * Send notification to a specific user (collaborator)
     * @param userId The userId of the collaborator
     * @param doctorId The associated doctor's ID
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type
     * @return Created notification
     */
    NotificationResponseDTO sendNotificationToUser(String userId, String doctorId, String title, String message, NotificationType type);

    /**
     * Send notification to doctor only
     * @param doctorId The doctor's ID
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type
     * @return Created notification
     */
    NotificationResponseDTO sendNotificationToDoctor(String doctorId, String title, String message, NotificationType type);

    /**
     * Send notification to doctor and all collaborators
     * @param doctorId The doctor's ID
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type
     * @return Created notification
     */
    NotificationResponseDTO sendNotificationToDoctorAndCollaborators(String doctorId, String title, String message, NotificationType type);

    /**
     * Send notification to all collaborators of a doctor (not visible to doctor)
     * @param doctorId The doctor's ID
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type
     * @return Created notification
     */
    NotificationResponseDTO sendNotificationToAllCollaborators(String doctorId, String title, String message, NotificationType type);

    /**
     * Send notification to admin users only
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type
     * @return Created notification
     */
    NotificationResponseDTO sendNotificationToAdmin(String title, String message, NotificationType type);

    /**
     * Broadcast notification to everyone (doctors, collaborators, admins)
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type
     * @return Created notification
     */
    NotificationResponseDTO broadcastNotification(String title, String message, NotificationType type);
}

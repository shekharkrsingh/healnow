package com.heal.doctor.dto;

import com.heal.doctor.models.enums.NotificationType;

import java.time.Instant;

public class NotificationResponseDTO {
    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean isRead = false;
    private Instant createdAt;
}

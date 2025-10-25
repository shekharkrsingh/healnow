package com.heal.doctor.dto;

import com.heal.doctor.models.enums.NotificationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

public class NotificationResponseDTO {
    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean isRead = false;
    private Instant createdAt;
}

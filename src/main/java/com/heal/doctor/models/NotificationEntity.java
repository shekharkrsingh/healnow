package com.heal.doctor.models;

import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    private String id;

    @Size(max = 100, message = "Target ID must not exceed 100 characters")
    private String targetId;

    @Size(max = 50, message = "Title must not exceed 50 characters")
    private String title;

    @NotNull(message = "Message is required")
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    private String message;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Size(max = 100, message = "Sender ID must not exceed 100 characters")
    private String senderId;

    @NotNull(message = "Recipient type is required")
    private NotificationRecipientType recipientType;

    private String link;

    private Instant createdAt;

    private Instant expiryDate;
}

package com.heal.doctor.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "user_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(
        name = "user_notification_unique",
        def = "{'userId': 1, 'notificationId': 1}",
        unique = true
)
@CompoundIndex(
        name = "user_read_index",
        def = "{'userId': 1, 'isRead': 1}"
)
public class UserNotification {

    @Id
    private String id;

    @NotNull
    @NotBlank(message = "User ID cannot be blank")
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;

    @NotNull
    @NotBlank(message = "Notification ID cannot be blank")
    private String notificationId;

    @Builder.Default
    private Boolean isRead = false;

    private Instant readAt;

    private Instant deliveredAt;
}


package com.heal.doctor.models;

import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.NotificationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "doctor_read_created_idx", def = "{'doctorId': 1, 'isRead': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "doctor_read_idx", def = "{'doctorId': 1, 'isRead': 1}")
})
public class NotificationEntity {

    private static final int NOTIFICATION_EXPIRY_DAYS = 7;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int HOURS_PER_DAY = 24;

    @Id
    private String id;

    @Indexed(name = "doctor_id_idx")
    @Size(max = 50, message = "Doctor ID must not exceed 50 characters")
    private String doctorId; // Required for DOCTOR_ONLY, DOCTOR_AND_COLLABORATORS, ALL_COLLABORATORS, USER_SPECIFIC

    @Indexed(name = "user_id_idx")
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId; // Required for USER_SPECIFIC, null for others

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Builder.Default
    @NotNull(message = "Target type is required")
    private NotificationTargetType targetType = NotificationTargetType.DOCTOR_AND_COLLABORATORS; // Default: visible to doctor and all collaborators

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotNull(message = "Message is required")
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    private String message;

    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    private Instant createdAt;

    @Indexed(name = "expiration_time_index", expireAfter = "0s")
    @Builder.Default
    private Instant expiryDate = Instant.now().plusSeconds((long) NOTIFICATION_EXPIRY_DAYS * HOURS_PER_DAY * SECONDS_PER_HOUR);
}

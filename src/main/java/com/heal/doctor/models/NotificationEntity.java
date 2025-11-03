package com.heal.doctor.models;

import com.heal.doctor.models.enums.NotificationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    private String id;

    @Indexed
    private String doctorId;

    @NotNull
    private NotificationType type;

    private String title;

    @NotNull
    @NotEmpty
    private String message;

    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    private Instant createdAt;

    @Indexed(name = "expiration_time_index", expireAfter = "0s")
    @Builder.Default
    private Instant expiryDate = Instant.now().plusSeconds(7 * 24 * 60 * 60);
}

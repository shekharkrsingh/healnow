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

    @Id
    private String id;

    @Indexed(name = "doctor_id_idx")
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
    private Instant expiryDate = Instant.now().plusSeconds(7L * 24 * 60 * 60);
}

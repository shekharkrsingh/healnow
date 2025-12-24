package com.heal.doctor.repositories;

import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationRecipientType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationEntity, String> {

    List<NotificationEntity> findByTargetIdOrderByCreatedAtDesc(String targetId);

    List<NotificationEntity> findByRecipientTypeOrderByCreatedAtDesc(NotificationRecipientType recipientType);

    void deleteByCreatedAtBefore(Instant cutoffDate);
}

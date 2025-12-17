package com.heal.doctor.repositories;

import com.heal.doctor.models.UserNotification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface UserNotificationRepository extends MongoRepository<UserNotification, String> {

    List<UserNotification> findByUserIdOrderByDeliveredAtDesc(String userId);

    List<UserNotification> findByUserIdAndIsReadFalseOrderByDeliveredAtDesc(String userId);

    java.util.Optional<UserNotification> findByUserIdAndNotificationId(String userId, String notificationId);

    void deleteByDeliveredAtBefore(Instant cutoffDate);
}

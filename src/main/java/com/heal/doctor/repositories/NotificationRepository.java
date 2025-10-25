package com.heal.doctor.repositories;

import com.heal.doctor.models.NotificationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends MongoRepository<NotificationEntity, String> {
    List<NotificationEntity> findByDoctorIdOrDoctorIdIsNullAndExpiryDateAfterOrderByCreatedAtDesc(String doctorId, Instant now);

    List<NotificationEntity> findByIsReadFalseAndDoctorIdOrDoctorIdIsNullAndExpiryDateAfter(String doctorId, Instant now);

    List<NotificationEntity> findByIsReadFalseAndDoctorIdAndExpiryDateAfter(String doctorId, Instant now);
}

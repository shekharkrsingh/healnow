package com.heal.doctor.repositories;

import com.heal.doctor.models.NotificationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationEntity, String> {
    List<NotificationEntity> findByDoctorIdOrDoctorIdIsNullOrderByCreatedAtDesc(String doctorId);

    List<NotificationEntity> findByIsReadFalseAndDoctorIdOrDoctorIdIsNull(String doctorId);

    List<NotificationEntity> findByIsReadFalseAndDoctorIdOrderByCreatedAtDesc(String doctorId);

    Optional<NotificationEntity> findByIdAndDoctorId(String id, String doctorId);
}

package com.heal.doctor.repositories;

import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationTargetType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationEntity, String> {
    List<NotificationEntity> findByDoctorIdOrDoctorIdIsNullOrderByCreatedAtDesc(String doctorId);

    List<NotificationEntity> findByIsReadFalseAndDoctorIdOrDoctorIdIsNull(String doctorId);

    List<NotificationEntity> findByIsReadFalseAndDoctorIdOrderByCreatedAtDesc(String doctorId);

    Optional<NotificationEntity> findByIdAndDoctorId(String id, String doctorId);

    // Query for notifications visible to a specific user (doctor, collaborator, or admin)
    // Note: MongoDB doesn't support boolean parameters in queries directly, so we'll use a different approach
    // This method will be called with different parameters based on user role
    @Query("{ $or: [ " +
           "{ targetType: 'USER_SPECIFIC', userId: ?0 }, " +
           "{ targetType: 'DOCTOR_AND_COLLABORATORS', doctorId: ?1 }, " +
           "{ targetType: 'BROADCAST' } " +
           "] }")
    List<NotificationEntity> findVisibleNotificationsForUserOrderByCreatedAtDesc(
            String userId, String doctorId);

    // Query for unread notifications visible to a specific user
    @Query("{ isRead: false, $or: [ " +
           "{ targetType: 'USER_SPECIFIC', userId: ?0 }, " +
           "{ targetType: 'DOCTOR_AND_COLLABORATORS', doctorId: ?1 }, " +
           "{ targetType: 'BROADCAST' } " +
           "] }")
    List<NotificationEntity> findUnreadVisibleNotificationsForUserOrderByCreatedAtDesc(
            String userId, String doctorId);

    // Find notification by ID that's visible to the user (basic query - filtering by role happens in service)
    @Query("{ _id: ?0, $or: [ " +
           "{ targetType: 'USER_SPECIFIC', userId: ?1 }, " +
           "{ targetType: 'DOCTOR_AND_COLLABORATORS', doctorId: ?2 }, " +
           "{ targetType: 'BROADCAST' } " +
           "] }")
    Optional<NotificationEntity> findByIdAndVisibleToUserBasic(
            String id, String userId, String doctorId);
    
    // Additional queries for specific target types
    List<NotificationEntity> findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType targetType, String doctorId);
    List<NotificationEntity> findByTargetTypeAndUserIdOrderByCreatedAtDesc(NotificationTargetType targetType, String userId);
    List<NotificationEntity> findByTargetTypeOrderByCreatedAtDesc(NotificationTargetType targetType);

    // Find all notifications for a specific doctorId (for doctor's view)
    @Query("{ $or: [ " +
           "{ doctorId: ?0 }, " +
           "{ targetType: 'BROADCAST' } " +
           "] }")
    List<NotificationEntity> findByDoctorIdOrBroadcastOrderByCreatedAtDesc(String doctorId);

    // Find all notifications for all collaborators of a doctor
    @Query("{ doctorId: ?0, $or: [ " +
           "{ targetType: 'DOCTOR_AND_COLLABORATORS' }, " +
           "{ targetType: 'ALL_COLLABORATORS' } " +
           "] }")
    List<NotificationEntity> findByDoctorIdForCollaboratorsOrderByCreatedAtDesc(String doctorId);

    // Find all admin-only notifications
    List<NotificationEntity> findByTargetTypeOrderByCreatedAtDesc(NotificationTargetType targetType);

    void deleteByCreatedAtBefore(Instant cutoffDate);
}

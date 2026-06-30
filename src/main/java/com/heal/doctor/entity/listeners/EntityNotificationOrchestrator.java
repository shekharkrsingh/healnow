package com.heal.doctor.entity.listeners;

import com.heal.doctor.entity.events.AffiliationAdminApprovedEvent;
import com.heal.doctor.entity.events.AffiliationPeerAcceptedEvent;
import com.heal.doctor.entity.events.AffiliationRejectedEvent;
import com.heal.doctor.entity.events.AffiliationRequestedEvent;
import com.heal.doctor.entity.events.AffiliationTerminatedEvent;
import com.heal.doctor.entity.events.AffiliationAvailabilityChangedEvent;
import com.heal.doctor.entity.events.StaffAssignedEvent;
import com.heal.doctor.entity.events.StaffRevokedEvent;
import com.heal.doctor.entity.models.NotificationContext;
import com.heal.doctor.entity.models.enums.AffiliationInitiator;
import com.heal.doctor.entity.models.enums.NotificationScope;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.services.INotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EntityNotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EntityNotificationOrchestrator.class);

    private final INotificationService notificationService;
    private final EntityAffiliationRepository affiliationRepository;

    @EventListener
    @Async("notificationTaskExecutor")
    public void onAffiliationRequested(AffiliationRequestedEvent event) {
        try {
            String targetId = event.initiatedBy() == AffiliationInitiator.DOCTOR
                    ? event.entityId()
                    : event.doctorId();

            NotificationRecipientType recipientType = event.initiatedBy() == AffiliationInitiator.DOCTOR
                    ? NotificationRecipientType.ENTITY_MEMBERS
                    : NotificationRecipientType.INDIVIDUAL;

            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(targetId)
                    .title("New Affiliation Request")
                    .message("A new affiliation request has been submitted.")
                    .type(NotificationType.INFO)
                    .recipientType(recipientType)
                    .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                    .createdAt(Instant.now())
                    .build();

            notificationService.createNotificationAsync(notification);
        } catch (Exception e) {
            log.error("Failed to send notification for AffiliationRequestedEvent: {}", e.getMessage());
        }
    }

    @EventListener
    @Async("notificationTaskExecutor")
    public void onPeerAccepted(AffiliationPeerAcceptedEvent event) {
        try {
            affiliationRepository.findByAffiliationId(event.affiliationId()).ifPresent(aff -> {
                NotificationEntity notification = NotificationEntity.builder()
                        .targetId(event.affiliationId())
                        .title("Affiliation Request Accepted")
                        .message("The affiliation request has been accepted and is pending admin approval.")
                        .type(NotificationType.INFO)
                        .recipientType(NotificationRecipientType.AFFILIATION_PARTIES)
                        .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                        .createdAt(Instant.now())
                        .build();
                notificationService.createNotificationAsync(notification);
            });
        } catch (Exception e) {
            log.error("Failed to send notification for AffiliationPeerAcceptedEvent: {}", e.getMessage());
        }
    }

    @EventListener
    @Async("notificationTaskExecutor")
    public void onAdminApproved(AffiliationAdminApprovedEvent event) {
        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(event.affiliationId())
                    .title("Affiliation Approved")
                    .message("Your affiliation has been approved and is now ACTIVE.")
                    .type(NotificationType.INFO)
                    .recipientType(NotificationRecipientType.AFFILIATION_PARTIES)
                    .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                    .createdAt(Instant.now())
                    .build();
            notificationService.createNotificationAsync(notification);
        } catch (Exception e) {
            log.error("Failed to send notification for AffiliationAdminApprovedEvent: {}", e.getMessage());
        }
    }

    @EventListener
    @Async("notificationTaskExecutor")
    public void onRejected(AffiliationRejectedEvent event) {
        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(event.affiliationId())
                    .title("Affiliation Rejected")
                    .message("The affiliation request was rejected. Reason: " + event.reason())
                    .type(NotificationType.INFO)
                    .recipientType(NotificationRecipientType.AFFILIATION_PARTIES)
                    .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                    .createdAt(Instant.now())
                    .build();
            notificationService.createNotificationAsync(notification);
        } catch (Exception e) {
            log.error("Failed to send notification for AffiliationRejectedEvent: {}", e.getMessage());
        }
    }

    @EventListener
    @Async("notificationTaskExecutor")
    public void onTerminated(AffiliationTerminatedEvent event) {
        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(event.affiliationId())
                    .title("Affiliation Terminated")
                    .message("The affiliation has been terminated. Reason: " + event.reason())
                    .type(NotificationType.INFO)
                    .recipientType(NotificationRecipientType.AFFILIATION_PARTIES)
                    .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                    .createdAt(Instant.now())
                    .build();
            notificationService.createNotificationAsync(notification);
        } catch (Exception e) {
            log.error("Failed to send notification for AffiliationTerminatedEvent: {}", e.getMessage());
        }
    }

    @EventListener
    @Async("notificationTaskExecutor")
    public void onAvailabilityChanged(AffiliationAvailabilityChangedEvent event) {
        try {
            affiliationRepository.findByAffiliationId(event.affiliationId()).ifPresent(aff -> {
                NotificationEntity notification = NotificationEntity.builder()
                        .targetId(aff.getDoctorId())
                        .title("Availability Updated")
                        .message("Your entity-scoped availability has been updated by the entity.")
                        .type(NotificationType.INFO)
                        .recipientType(NotificationRecipientType.INDIVIDUAL)
                        .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                        .createdAt(Instant.now())
                        .build();
                notificationService.createNotificationAsync(notification);
            });
        } catch (Exception e) {
            log.error("Failed to send notification for AffiliationAvailabilityChangedEvent: {}", e.getMessage());
        }
    }

    @EventListener
    @Async("notificationTaskExecutor")
    public void onStaffAssigned(StaffAssignedEvent event) {
        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(event.userId())
                    .title("Staff Assignment")
                    .message("You have been assigned as staff to an affiliation.")
                    .type(NotificationType.INFO)
                    .recipientType(NotificationRecipientType.INDIVIDUAL)
                    .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                    .createdAt(Instant.now())
                    .build();
            notificationService.createNotificationAsync(notification);
        } catch (Exception e) {
            log.error("Failed to send notification for StaffAssignedEvent: {}", e.getMessage());
        }
    }

    @EventListener
    @Async("notificationTaskExecutor")
    public void onStaffRevoked(StaffRevokedEvent event) {
        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(event.userId())
                    .title("Staff Access Revoked")
                    .message("Your staff access to an affiliation has been revoked.")
                    .type(NotificationType.INFO)
                    .recipientType(NotificationRecipientType.INDIVIDUAL)
                    .notificationContext(buildContext(event.affiliationId(), event.entityId(), event.doctorId(), NotificationScope.AFFILIATION))
                    .createdAt(Instant.now())
                    .build();
            notificationService.createNotificationAsync(notification);
        } catch (Exception e) {
            log.error("Failed to send notification for StaffRevokedEvent: {}", e.getMessage());
        }
    }

    private NotificationContext buildContext(String affiliationId, String entityId, String doctorId, NotificationScope scope) {
        return NotificationContext.builder()
                .affiliationId(affiliationId)
                .entityId(entityId)
                .doctorId(doctorId)
                .scope(scope)
                .build();
    }
}

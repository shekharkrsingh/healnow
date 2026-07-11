package com.heal.doctor.services.impl;

import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.dto.WebSocketResponseType;
import com.heal.doctor.dto.WebsocketResponseDTO;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.UserNotification;
import com.heal.doctor.models.enums.CollaboratorStatus;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.NotificationRepository;
import com.heal.doctor.repositories.UserNotificationRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationResponseDTO createNotification(NotificationEntity notification) {
        logger.debug("Creating notification: type: {}, title: {}, recipientType: {}",
                notification.getType(), notification.getTitle(), notification.getRecipientType());

        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }

        NotificationEntity savedNotification = notificationRepository.save(notification);
        logger.info("Notification master record created: {}", savedNotification.getId());
        
        List<String> recipientIds = resolveRecipients(notification);
        logger.info("Resolved {} recipients for notification {}", recipientIds.size(), savedNotification.getId());

        List<UserNotification> userNotifications = new ArrayList<>();
        NotificationResponseDTO responseDTO = modelMapper.map(savedNotification, NotificationResponseDTO.class);
        responseDTO.setIsRead(false);

        for (String userId : recipientIds) {
            UserNotification userNotification = UserNotification.builder()
                    .userId(userId)
                    .notificationId(savedNotification.getId())
                    .isRead(false)
                    .deliveredAt(Instant.now())
                    .build();
            userNotifications.add(userNotification);

            messagingTemplate.convertAndSend("/topic/notifications/" + userId,
                    WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                            .type(WebSocketResponseType.NOTIFICATION)
                            .payload(responseDTO)
                            .build());
        }
        
        if (!userNotifications.isEmpty()) {
            userNotificationRepository.saveAll(userNotifications);
        }

        return responseDTO;
    }

    @Override
    @Async("notificationTaskExecutor")
    public CompletableFuture<Void> createNotificationAsync(NotificationEntity notification) {
        try {
            createNotification(notification);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to create notification asynchronously: {}", e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private List<String> resolveRecipients(NotificationEntity notification) {
        List<String> recipientIds = new ArrayList<>();
        NotificationRecipientType type = notification.getRecipientType();
        String targetId = notification.getTargetId();

        switch (type) {
            case INDIVIDUAL:
                if (targetId != null) recipientIds.add(targetId);
                break;
            case ROLE:
                if (targetId != null) {
                    try {
                        RolesEnum role = RolesEnum.valueOf(targetId.toUpperCase());
                        List<UserEntity> users = userRepository.findByRolesEnumAndIsActiveTrue(role);
                        users.forEach(u -> recipientIds.add(u.getUserId()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid role specified for notification: {}", targetId);
                    }
                }
                break;
            case BROADCAST:
                userRepository.findAllByIsActiveTrue().forEach(u -> recipientIds.add(u.getUserId()));
                break;
            case DOCTOR_COLLABORATORS:
                if (targetId != null) {
                    // Required new NotificationRecipent as DOCTOR_AND_COLLABORATORS for seprate funcnality doctor and collaborator
                    recipientIds.add(targetId); // Include the doctor themselves
                    
                    List<CollaboratorProfileEntity> collaborators = collaboratorProfileRepository
                            .findByDoctorAssociations_DoctorIdAndDoctorAssociations_Active(targetId, true);

                    collaborators.stream()
                            .filter(c -> c.getStatus() == CollaboratorStatus.ACTIVATED)
                            .forEach(c -> recipientIds.add(c.getCollaboratorId()));
                }
                break;
            case ADMINS:
                userRepository.findByRolesEnumAndIsActiveTrue(RolesEnum.ADMIN).forEach(u -> recipientIds.add(u.getUserId()));
                break;
            default:
                break;
        }
        return recipientIds.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponseDTO> getAllNotifications() {
        String userId = getCurrentUserId(); 
        
        List<UserNotification> userNotifications = userNotificationRepository.findByUserIdOrderByDeliveredAtDesc(userId);
        return mapToDTOs(userNotifications);
    }

    @Override
    public List<NotificationResponseDTO> getUnreadNotifications() {
        String userId = getCurrentUserId();
        List<UserNotification> userNotifications = userNotificationRepository.findByUserIdAndIsReadFalseOrderByDeliveredAtDesc(userId);
        return mapToDTOs(userNotifications);
    }

    @Override
    public NotificationResponseDTO markAsRead(String notificationId) {
        String userId = getCurrentUserId();
        UserNotification userNotification = userNotificationRepository.findByUserIdAndNotificationId(userId, notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        
        userNotification.setIsRead(true);
        userNotification.setReadAt(Instant.now());
        userNotificationRepository.save(userNotification);

        NotificationEntity notificationEntity = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationMaster", notificationId));



        NotificationResponseDTO dto = modelMapper.map(notificationEntity, NotificationResponseDTO.class);
        dto.setIsRead(true);

        messagingTemplate.convertAndSend("/topic/notifications/" + userId,
                WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                        .type(WebSocketResponseType.NOTIFICATION)
                        .payload(dto)
                        .build());

        return dto;
    }

    @Override
    public List<NotificationResponseDTO> markAllAsRead() {
        String userId = getCurrentUserId();
        List<UserNotification> unread = userNotificationRepository.findByUserIdAndIsReadFalseOrderByDeliveredAtDesc(userId);
        
        unread.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(Instant.now());
        });
        userNotificationRepository.saveAll(unread);
        
        List<NotificationResponseDTO> updatedDTOs = mapToDTOs(unread);
        updatedDTOs.forEach(dto -> 
            messagingTemplate.convertAndSend("/topic/notifications/" + userId,
                WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                        .type(WebSocketResponseType.NOTIFICATION)
                        .payload(dto)
                        .build())
        );

        return updatedDTOs;
    }

    private List<NotificationResponseDTO> mapToDTOs(List<UserNotification> userNotifications) {
        if (userNotifications.isEmpty()) return Collections.emptyList();

        List<String> notificationIds = userNotifications.stream()
                .map(UserNotification::getNotificationId)
                .collect(Collectors.toList());

        Map<String, NotificationEntity> notificationMap = notificationRepository.findAllById(notificationIds).stream()
                .collect(Collectors.toMap(NotificationEntity::getId, n -> n));

        return userNotifications.stream()
                .filter(un -> notificationMap.containsKey(un.getNotificationId()))
                .map(un -> {
                    NotificationEntity entity = notificationMap.get(un.getNotificationId());
                    NotificationResponseDTO dto = modelMapper.map(entity, NotificationResponseDTO.class);
                    dto.setIsRead(un.getIsRead());
                    return dto;
                })
                .collect(Collectors.toList());
    }


    private String getCurrentUserId() {
        return CurrentUserName.getCurrentUserId();
    }
}

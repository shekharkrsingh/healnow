package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.dto.WebSocketResponseType;
import com.heal.doctor.dto.WebsocketResponseDTO;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.repositories.NotificationRepository;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.utils.CurrentUserName;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService implements INotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;


    public NotificationService(NotificationRepository notificationRepository, ModelMapper modelMapper, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public NotificationResponseDTO createNotification(NotificationEntity notification) {
        logger.debug("Creating notification: doctorId: {}, type: {}, title: {}", 
                notification.getDoctorId(), notification.getType(), notification.getTitle());
        NotificationEntity savedNotification= notificationRepository.save(notification);
        logger.info("Notification created: notificationId: {}, doctorId: {}, type: {}", 
                savedNotification.getId(), savedNotification.getDoctorId(), savedNotification.getType());
        NotificationResponseDTO notificationResponseDTO=modelMapper.map(savedNotification, NotificationResponseDTO.class);
        if(!notification.getType().equals(NotificationType.SYSTEM)){
            String doctorId = CurrentUserName.getCurrentDoctorId();
            logger.debug("Sending WebSocket notification: doctorId: {}, notificationId: {}", 
                    doctorId, savedNotification.getId());
            messagingTemplate.convertAndSend("/topic/appointments/" + doctorId,
                    WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                    .type(WebSocketResponseType.NOTIFICATION)
                    .payload(notificationResponseDTO)
                    .build());
        }
        return notificationResponseDTO;
    }

    @Override
    @Async("notificationTaskExecutor")
    public CompletableFuture<Void> createNotificationAsync(NotificationEntity notification) {
        logger.debug("Creating notification asynchronously: doctorId: {}, type: {}, title: {}", 
                notification.getDoctorId(), notification.getType(), notification.getTitle());
        try {
            NotificationEntity savedNotification = notificationRepository.save(notification);
            logger.info("Notification created asynchronously: notificationId: {}, doctorId: {}, type: {}", 
                    savedNotification.getId(), savedNotification.getDoctorId(), savedNotification.getType());
            NotificationResponseDTO notificationResponseDTO = modelMapper.map(savedNotification, NotificationResponseDTO.class);
            if(!notification.getType().equals(NotificationType.SYSTEM)){
                String doctorId = savedNotification.getDoctorId();
                logger.debug("Sending WebSocket notification asynchronously: doctorId: {}, notificationId: {}", 
                        doctorId, savedNotification.getId());
                messagingTemplate.convertAndSend("/topic/appointments/" + doctorId,
                        WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                        .type(WebSocketResponseType.NOTIFICATION)
                        .payload(notificationResponseDTO)
                        .build());
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to create notification asynchronously: doctorId: {}, type: {}, error: {}", 
                    notification.getDoctorId(), notification.getType(), e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public List<NotificationResponseDTO> getAllNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching all notifications: doctorId: {}", doctorId);
        List<NotificationEntity> notifications = notificationRepository.findByDoctorIdOrDoctorIdIsNullOrderByCreatedAtDesc(doctorId);
        logger.debug("Found {} notifications for doctorId: {}", notifications.size(), doctorId);
        return notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

    @Override
    public List<NotificationResponseDTO> getUnreadNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching unread notifications: doctorId: {}", doctorId);
        List<NotificationEntity> notifications = notificationRepository.findByIsReadFalseAndDoctorIdOrderByCreatedAtDesc(doctorId);
        logger.debug("Found {} unread notifications for doctorId: {}", notifications.size(), doctorId);
        return notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

    @Override
    public NotificationResponseDTO markAsRead(String notificationId) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Marking notification as read: notificationId: {}, doctorId: {}", notificationId, doctorId);
        NotificationEntity notification = notificationRepository.findByIdAndDoctorId(notificationId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.setIsRead(true);
        NotificationEntity updatedNotification= notificationRepository.save(notification);
        logger.debug("Notification marked as read: notificationId: {}, doctorId: {}", notificationId, doctorId);
        return modelMapper.map(updatedNotification, NotificationResponseDTO.class);
    }

    @Override
    public List<NotificationResponseDTO> markAllAsReadForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Marking all notifications as read: doctorId: {}", doctorId);
        List<NotificationEntity> notifications = notificationRepository
                .findByIsReadFalseAndDoctorIdOrderByCreatedAtDesc(doctorId);
        notifications.forEach(n -> {
            n.setIsRead(true);
        });
        List<NotificationEntity> savedNotification = notificationRepository.saveAll(notifications);
        logger.info("Marked {} notifications as read for doctorId: {}", savedNotification.size(), doctorId);
        return savedNotification.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

}

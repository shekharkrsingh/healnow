package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.dto.WebSocketResponseType;
import com.heal.doctor.dto.WebsocketResponseDTO;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.repositories.NotificationRepository;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.utils.CurrentUserName;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService implements INotificationService {

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
        NotificationEntity savedNotification= notificationRepository.save(notification);
        NotificationResponseDTO notificationResponseDTO=modelMapper.map(savedNotification, NotificationResponseDTO.class);
        if(!notification.getType().equals(NotificationType.SYSTEM)){
            messagingTemplate.convertAndSend("/topic/appointments/" + CurrentUserName.getCurrentDoctorId(),
                    WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                    .type(WebSocketResponseType.NOTIFICATION)
                    .payload(notificationResponseDTO)
                    .build());
        }
        return notificationResponseDTO;
    }

    @Override
    public List<NotificationResponseDTO> getAllNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        Instant now = Instant.now();
        List<NotificationEntity> notifications= notificationRepository.findByDoctorIdOrDoctorIdIsNullOrderByCreatedAtDesc(doctorId);
        return notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

    @Override
    public List<NotificationResponseDTO> getUnreadNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        Instant now = Instant.now();
        List<NotificationEntity> notifications= notificationRepository.findByIsReadFalseAndDoctorIdOrDoctorIdIsNull(doctorId);
        return notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

    @Override
    public NotificationResponseDTO markAsRead(String notificationId) {
        NotificationEntity notification = notificationRepository.findByIdAndDoctorId(notificationId, CurrentUserName.getCurrentDoctorId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        NotificationEntity updatedNotification= notificationRepository.save(notification);
        return modelMapper.map(updatedNotification, NotificationResponseDTO.class);
    }

    @Override
    public List<NotificationResponseDTO> markAllAsReadForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        List<NotificationEntity> notifications = notificationRepository
                .findByIsReadFalseAndDoctorId(doctorId);
        notifications.forEach(n -> {
            n.setIsRead(true);
        });
        List<NotificationEntity> savedNotification= notificationRepository.saveAll(notifications);
        return savedNotification.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

}

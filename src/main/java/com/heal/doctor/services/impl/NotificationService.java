package com.heal.doctor.services.impl;

import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.repositories.NotificationRepository;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.utils.CurrentUserName;
import org.modelmapper.ModelMapper;

import java.time.Instant;
import java.util.List;

public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    public NotificationService(NotificationRepository notificationRepository, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public NotificationResponseDTO createNotification(NotificationEntity notification) {
        NotificationEntity savedNotification= notificationRepository.save(notification);
        return modelMapper.map(savedNotification, NotificationResponseDTO.class);
    }

    @Override
    public List<NotificationResponseDTO> getAllNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        Instant now = Instant.now();
        List<NotificationEntity> notifications= notificationRepository.findByDoctorIdOrDoctorIdIsNullAndExpiryDateAfterOrderByCreatedAtDesc(doctorId, now);
        return notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

    @Override
    public List<NotificationResponseDTO> getUnreadNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        Instant now = Instant.now();
        List<NotificationEntity> notifications= notificationRepository.findByIsReadFalseAndDoctorIdOrDoctorIdIsNullAndExpiryDateAfter(doctorId, now);
        return notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .toList();
    }

    @Override
    public NotificationResponseDTO markAsRead(String notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return modelMapper.map(notification, NotificationResponseDTO.class);
    }

    @Override
    public void markAllAsReadForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        List<NotificationEntity> notifications = notificationRepository
                .findByIsReadFalseAndDoctorIdAndExpiryDateAfter(doctorId, Instant.now());
        notifications.forEach(n -> {
            n.setIsRead(true);
        });
        notificationRepository.saveAll(notifications);
    }

}

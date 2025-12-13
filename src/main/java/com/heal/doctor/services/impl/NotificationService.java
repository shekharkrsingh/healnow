package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.NotificationResponseDTO;
import com.heal.doctor.dto.WebSocketResponseType;
import com.heal.doctor.dto.WebsocketResponseDTO;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.NotificationTargetType;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.repositories.NotificationRepository;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.utils.CurrentUserName;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
        logger.debug("Creating notification: doctorId: {}, userId: {}, targetType: {}, type: {}, title: {}", 
                notification.getDoctorId(), notification.getUserId(), notification.getTargetType(), 
                notification.getType(), notification.getTitle());
        NotificationEntity savedNotification = notificationRepository.save(notification);
        logger.info("Notification created: notificationId: {}, doctorId: {}, userId: {}, targetType: {}, type: {}", 
                savedNotification.getId(), savedNotification.getDoctorId(), savedNotification.getUserId(), 
                savedNotification.getTargetType(), savedNotification.getType());
        NotificationResponseDTO notificationResponseDTO = modelMapper.map(savedNotification, NotificationResponseDTO.class);
        
        if(!notification.getType().equals(NotificationType.SYSTEM)){
            // Send WebSocket notification based on target type
            NotificationTargetType targetType = savedNotification.getTargetType();
            String doctorId = savedNotification.getDoctorId();
            String userId = savedNotification.getUserId();
            
            if (targetType == NotificationTargetType.BROADCAST) {
                // Broadcast to all - send to a global topic
                logger.debug("Sending broadcast WebSocket notification: notificationId: {}", savedNotification.getId());
                messagingTemplate.convertAndSend("/topic/notifications/broadcast",
                        WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                        .type(WebSocketResponseType.NOTIFICATION)
                        .payload(notificationResponseDTO)
                        .build());
            } else if (targetType == NotificationTargetType.ADMIN_ONLY) {
                // Send to admin topic
                logger.debug("Sending admin-only WebSocket notification: notificationId: {}", savedNotification.getId());
                messagingTemplate.convertAndSend("/topic/notifications/admin",
                        WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                        .type(WebSocketResponseType.NOTIFICATION)
                        .payload(notificationResponseDTO)
                        .build());
            } else if (targetType == NotificationTargetType.USER_SPECIFIC && userId != null) {
                // Send to specific user's topic
                logger.debug("Sending user-specific WebSocket notification: userId: {}, notificationId: {}", 
                        userId, savedNotification.getId());
                messagingTemplate.convertAndSend("/topic/notifications/user/" + userId,
                        WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                        .type(WebSocketResponseType.NOTIFICATION)
                        .payload(notificationResponseDTO)
                        .build());
            } else if (doctorId != null) {
                // Send to doctor's topic (for DOCTOR_ONLY, DOCTOR_AND_COLLABORATORS, ALL_COLLABORATORS)
                logger.debug("Sending WebSocket notification: doctorId: {}, notificationId: {}", 
                        doctorId, savedNotification.getId());
                messagingTemplate.convertAndSend("/topic/appointments/" + doctorId,
                        WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                        .type(WebSocketResponseType.NOTIFICATION)
                        .payload(notificationResponseDTO)
                        .build());
            }
        }
        return notificationResponseDTO;
    }

    @Override
    @Async("notificationTaskExecutor")
    public CompletableFuture<Void> createNotificationAsync(NotificationEntity notification) {
        logger.debug("Creating notification asynchronously: doctorId: {}, userId: {}, targetType: {}, type: {}, title: {}", 
                notification.getDoctorId(), notification.getUserId(), notification.getTargetType(), 
                notification.getType(), notification.getTitle());
        try {
            NotificationEntity savedNotification = notificationRepository.save(notification);
            logger.info("Notification created asynchronously: notificationId: {}, doctorId: {}, userId: {}, targetType: {}, type: {}", 
                    savedNotification.getId(), savedNotification.getDoctorId(), savedNotification.getUserId(), 
                    savedNotification.getTargetType(), savedNotification.getType());
            NotificationResponseDTO notificationResponseDTO = modelMapper.map(savedNotification, NotificationResponseDTO.class);
            
            if(!notification.getType().equals(NotificationType.SYSTEM)){
                // Send WebSocket notification based on target type
                NotificationTargetType targetType = savedNotification.getTargetType();
                String doctorId = savedNotification.getDoctorId();
                String userId = savedNotification.getUserId();
                
                if (targetType == NotificationTargetType.BROADCAST) {
                    messagingTemplate.convertAndSend("/topic/notifications/broadcast",
                            WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                            .type(WebSocketResponseType.NOTIFICATION)
                            .payload(notificationResponseDTO)
                            .build());
                } else if (targetType == NotificationTargetType.ADMIN_ONLY) {
                    messagingTemplate.convertAndSend("/topic/notifications/admin",
                            WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                            .type(WebSocketResponseType.NOTIFICATION)
                            .payload(notificationResponseDTO)
                            .build());
                } else if (targetType == NotificationTargetType.USER_SPECIFIC && userId != null) {
                    messagingTemplate.convertAndSend("/topic/notifications/user/" + userId,
                            WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                            .type(WebSocketResponseType.NOTIFICATION)
                            .payload(notificationResponseDTO)
                            .build());
                } else if (doctorId != null) {
                    messagingTemplate.convertAndSend("/topic/appointments/" + doctorId,
                            WebsocketResponseDTO.<NotificationResponseDTO>builderGeneric()
                            .type(WebSocketResponseType.NOTIFICATION)
                            .payload(notificationResponseDTO)
                            .build());
                }
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

    /**
     * Helper method to check if current user is admin
     */
    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }

    /**
     * Helper method to get current user ID (for collaborators, this would be their userId)
     * For now, since only doctors exist, this returns doctorId
     */
    private String getCurrentUserId() {
        // For now, userId = doctorId since only doctors exist
        // When collaborators are added, this should extract actual userId from JWT or UserDetails
        return CurrentUserName.getCurrentDoctorId();
    }

    /**
     * Helper method to check if current user is doctor
     * Returns true if user has DOCTOR role (not ADMIN)
     */
    private boolean isCurrentUserDoctor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_DOCTOR"));
        }
        // If no authorities, check if principal is DoctorUserDetails (which means it's a doctor)
        if (authentication != null && authentication.getPrincipal() instanceof com.heal.doctor.security.DoctorUserDetails) {
            return true;
        }
        return true; // Default to doctor if no authorities found
    }

    @Override
    public List<NotificationResponseDTO> getAllNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        String userId = getCurrentUserId();
        boolean isDoctor = isCurrentUserDoctor();
        boolean isAdmin = isCurrentUserAdmin();
        
        logger.debug("Fetching all notifications: doctorId: {}, userId: {}, isDoctor: {}, isAdmin: {}", 
                doctorId, userId, isDoctor, isAdmin);
        
        // Get base notifications (USER_SPECIFIC, DOCTOR_AND_COLLABORATORS, BROADCAST)
        List<NotificationEntity> notifications = notificationRepository
                .findVisibleNotificationsForUserOrderByCreatedAtDesc(userId, doctorId);
        
        // Add role-specific notifications
        if (isDoctor) {
            // Add DOCTOR_ONLY notifications
            List<NotificationEntity> doctorOnly = notificationRepository
                    .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.DOCTOR_ONLY, doctorId);
            notifications.addAll(doctorOnly);
        } else {
            // User is collaborator - add ALL_COLLABORATORS notifications
            List<NotificationEntity> allCollaborators = notificationRepository
                    .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.ALL_COLLABORATORS, doctorId);
            notifications.addAll(allCollaborators);
        }
        
        if (isAdmin) {
            // Add ADMIN_ONLY notifications
            List<NotificationEntity> adminOnly = notificationRepository
                    .findByTargetTypeOrderByCreatedAtDesc(NotificationTargetType.ADMIN_ONLY);
            notifications.addAll(adminOnly);
        }
        
        // Remove duplicates and sort by createdAt desc
        notifications = notifications.stream()
                .distinct()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        
        logger.debug("Found {} notifications for doctorId: {}, userId: {}", notifications.size(), doctorId, userId);
        return notifications.parallelStream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponseDTO> getUnreadNotificationsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        String userId = getCurrentUserId();
        boolean isDoctor = isCurrentUserDoctor();
        boolean isAdmin = isCurrentUserAdmin();
        
        logger.debug("Fetching unread notifications: doctorId: {}, userId: {}, isDoctor: {}, isAdmin: {}", 
                doctorId, userId, isDoctor, isAdmin);
        
        // Get base unread notifications
        List<NotificationEntity> notifications = notificationRepository
                .findUnreadVisibleNotificationsForUserOrderByCreatedAtDesc(userId, doctorId);
        
        // Add role-specific unread notifications
        if (isDoctor) {
            List<NotificationEntity> doctorOnly = notificationRepository
                    .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.DOCTOR_ONLY, doctorId)
                    .stream()
                    .filter(n -> !n.getIsRead())
                    .collect(Collectors.toList());
            notifications.addAll(doctorOnly);
        } else {
            List<NotificationEntity> allCollaborators = notificationRepository
                    .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.ALL_COLLABORATORS, doctorId)
                    .stream()
                    .filter(n -> !n.getIsRead())
                    .collect(Collectors.toList());
            notifications.addAll(allCollaborators);
        }
        
        if (isAdmin) {
            List<NotificationEntity> adminOnly = notificationRepository
                    .findByTargetTypeOrderByCreatedAtDesc(NotificationTargetType.ADMIN_ONLY)
                    .stream()
                    .filter(n -> !n.getIsRead())
                    .collect(Collectors.toList());
            notifications.addAll(adminOnly);
        }
        
        // Remove duplicates and sort
        notifications = notifications.stream()
                .distinct()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        
        logger.debug("Found {} unread notifications for doctorId: {}, userId: {}", notifications.size(), doctorId, userId);
        return notifications.parallelStream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponseDTO markAsRead(String notificationId) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        String userId = getCurrentUserId();
        boolean isDoctor = isCurrentUserDoctor();
        boolean isAdmin = isCurrentUserAdmin();
        
        logger.info("Marking notification as read: notificationId: {}, doctorId: {}, userId: {}, isDoctor: {}, isAdmin: {}", 
                notificationId, doctorId, userId, isDoctor, isAdmin);
        
        // Try to find notification with basic query first
        Optional<NotificationEntity> notificationOpt = notificationRepository
                .findByIdAndVisibleToUserBasic(notificationId, userId, doctorId);
        
        // If not found, check role-specific notifications
        if (notificationOpt.isEmpty()) {
            if (isDoctor) {
                notificationOpt = notificationRepository
                        .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.DOCTOR_ONLY, doctorId)
                        .stream()
                        .filter(n -> n.getId().equals(notificationId))
                        .findFirst();
            } else {
                notificationOpt = notificationRepository
                        .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.ALL_COLLABORATORS, doctorId)
                        .stream()
                        .filter(n -> n.getId().equals(notificationId))
                        .findFirst();
            }
        }
        
        if (notificationOpt.isEmpty() && isAdmin) {
            notificationOpt = notificationRepository
                    .findByTargetTypeOrderByCreatedAtDesc(NotificationTargetType.ADMIN_ONLY)
                    .stream()
                    .filter(n -> n.getId().equals(notificationId))
                    .findFirst();
        }
        
        NotificationEntity notification = notificationOpt
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        
        notification.setIsRead(true);
        NotificationEntity updatedNotification = notificationRepository.save(notification);
        logger.debug("Notification marked as read: notificationId: {}, doctorId: {}, userId: {}", 
                notificationId, doctorId, userId);
        return modelMapper.map(updatedNotification, NotificationResponseDTO.class);
    }

    @Override
    public List<NotificationResponseDTO> markAllAsReadForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        String userId = getCurrentUserId();
        boolean isDoctor = isCurrentUserDoctor();
        boolean isAdmin = isCurrentUserAdmin();
        
        logger.info("Marking all notifications as read: doctorId: {}, userId: {}, isDoctor: {}, isAdmin: {}", 
                doctorId, userId, isDoctor, isAdmin);
        
        // Get all unread notifications (same logic as getUnreadNotificationsForCurrentDoctor)
        List<NotificationEntity> notifications = notificationRepository
                .findUnreadVisibleNotificationsForUserOrderByCreatedAtDesc(userId, doctorId);
        
        // Add role-specific unread notifications
        if (isDoctor) {
            List<NotificationEntity> doctorOnly = notificationRepository
                    .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.DOCTOR_ONLY, doctorId)
                    .stream()
                    .filter(n -> !n.getIsRead())
                    .collect(Collectors.toList());
            notifications.addAll(doctorOnly);
        } else {
            List<NotificationEntity> allCollaborators = notificationRepository
                    .findByTargetTypeAndDoctorIdOrderByCreatedAtDesc(NotificationTargetType.ALL_COLLABORATORS, doctorId)
                    .stream()
                    .filter(n -> !n.getIsRead())
                    .collect(Collectors.toList());
            notifications.addAll(allCollaborators);
        }
        
        if (isAdmin) {
            List<NotificationEntity> adminOnly = notificationRepository
                    .findByTargetTypeOrderByCreatedAtDesc(NotificationTargetType.ADMIN_ONLY)
                    .stream()
                    .filter(n -> !n.getIsRead())
                    .collect(Collectors.toList());
            notifications.addAll(adminOnly);
        }
        
        // Remove duplicates
        notifications = notifications.stream()
                .distinct()
                .collect(Collectors.toList());
        
        notifications.forEach(n -> {
            n.setIsRead(true);
        });
        List<NotificationEntity> savedNotification = notificationRepository.saveAll(notifications);
        logger.info("Marked {} notifications as read for doctorId: {}, userId: {}", 
                savedNotification.size(), doctorId, userId);
        return savedNotification.parallelStream()
                .map(notification -> modelMapper.map(notification, NotificationResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponseDTO sendNotificationToUser(String userId, String doctorId, String title, String message, NotificationType type) {
        logger.info("Sending notification to specific user: userId: {}, doctorId: {}, title: {}", userId, doctorId, title);
        NotificationEntity notification = NotificationEntity.builder()
                .doctorId(doctorId)
                .userId(userId)
                .targetType(NotificationTargetType.USER_SPECIFIC)
                .type(type)
                .title(title)
                .message(message)
                .build();
        return createNotification(notification);
    }

    @Override
    public NotificationResponseDTO sendNotificationToDoctor(String doctorId, String title, String message, NotificationType type) {
        logger.info("Sending notification to doctor only: doctorId: {}, title: {}", doctorId, title);
        NotificationEntity notification = NotificationEntity.builder()
                .doctorId(doctorId)
                .userId(null)
                .targetType(NotificationTargetType.DOCTOR_ONLY)
                .type(type)
                .title(title)
                .message(message)
                .build();
        return createNotification(notification);
    }

    @Override
    public NotificationResponseDTO sendNotificationToDoctorAndCollaborators(String doctorId, String title, String message, NotificationType type) {
        logger.info("Sending notification to doctor and collaborators: doctorId: {}, title: {}", doctorId, title);
        NotificationEntity notification = NotificationEntity.builder()
                .doctorId(doctorId)
                .userId(null)
                .targetType(NotificationTargetType.DOCTOR_AND_COLLABORATORS)
                .type(type)
                .title(title)
                .message(message)
                .build();
        return createNotification(notification);
    }

    @Override
    public NotificationResponseDTO sendNotificationToAllCollaborators(String doctorId, String title, String message, NotificationType type) {
        logger.info("Sending notification to all collaborators: doctorId: {}, title: {}", doctorId, title);
        NotificationEntity notification = NotificationEntity.builder()
                .doctorId(doctorId)
                .userId(null)
                .targetType(NotificationTargetType.ALL_COLLABORATORS)
                .type(type)
                .title(title)
                .message(message)
                .build();
        return createNotification(notification);
    }

    @Override
    public NotificationResponseDTO sendNotificationToAdmin(String title, String message, NotificationType type) {
        logger.info("Sending notification to admin only: title: {}", title);
        NotificationEntity notification = NotificationEntity.builder()
                .doctorId(null)
                .userId(null)
                .targetType(NotificationTargetType.ADMIN_ONLY)
                .type(type)
                .title(title)
                .message(message)
                .build();
        return createNotification(notification);
    }

    @Override
    public NotificationResponseDTO broadcastNotification(String title, String message, NotificationType type) {
        logger.info("Broadcasting notification to everyone: title: {}", title);
        NotificationEntity notification = NotificationEntity.builder()
                .doctorId(null)
                .userId(null)
                .targetType(NotificationTargetType.BROADCAST)
                .type(type)
                .title(title)
                .message(message)
                .build();
        return createNotification(notification);
    }

}

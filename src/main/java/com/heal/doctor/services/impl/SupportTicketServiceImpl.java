package com.heal.doctor.services.impl;

import com.heal.doctor.dto.SupportTicketRequestDTO;
import com.heal.doctor.dto.SupportTicketResponseDTO;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.exception.UnauthorizedException;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.SupportTicketEntity;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.repositories.SupportTicketRepository;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.services.ISupportTicketService;
import com.heal.doctor.services.IEmailService;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.utils.CurrentUserName;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SupportTicketServiceImpl implements ISupportTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketServiceImpl.class);

    private final SupportTicketRepository supportTicketRepository;
    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;
    private final IEmailService emailService;
    private final INotificationService notificationService;

    @Value("${spring.mail.username}")
    private String supportEmail;

    @Autowired
    public SupportTicketServiceImpl(
            SupportTicketRepository supportTicketRepository,
            DoctorRepository doctorRepository,
            ModelMapper modelMapper,
            IEmailService emailService,
            INotificationService notificationService) {
        this.supportTicketRepository = supportTicketRepository;
        this.doctorRepository = doctorRepository;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public SupportTicketResponseDTO createSupportTicket(SupportTicketRequestDTO requestDTO) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Creating support ticket: doctorId: {}, category: {}, subject: {}", 
                doctorId, requestDTO.getCategory(), requestDTO.getSubject());

        String doctorEmail = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId))
                .getEmail();

        String ticketId = generateUniqueTicketId();

        SupportTicketEntity supportTicket = SupportTicketEntity.builder()
                .ticketId(ticketId)
                .doctorId(doctorId)
                .doctorEmail(doctorEmail)
                .subject(requestDTO.getSubject())
                .message(requestDTO.getMessage())
                .category(requestDTO.getCategory())
                .status("OPEN")
                .priority(determinePriority(requestDTO.getCategory()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SupportTicketEntity savedTicket = supportTicketRepository.save(supportTicket);
        logger.info("Support ticket created successfully: ticketId: {}, doctorId: {}", ticketId, doctorId);

        sendSupportTicketEmails(savedTicket);
        createSupportTicketNotification(savedTicket);

        return modelMapper.map(savedTicket, SupportTicketResponseDTO.class);
    }

    @Override
    public SupportTicketResponseDTO getSupportTicketById(String ticketId) {
        logger.debug("Fetching support ticket: ticketId: {}", ticketId);
        SupportTicketEntity ticket = supportTicketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket", ticketId));

        String currentDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!ticket.getDoctorId().equals(currentDoctorId)) {
            throw new UnauthorizedException("You do not have access to this support ticket");
        }

        return modelMapper.map(ticket, SupportTicketResponseDTO.class);
    }

    @Override
    public List<SupportTicketResponseDTO> getAllSupportTicketsForCurrentDoctor() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching all support tickets for doctor: doctorId: {}", doctorId);
        
        List<SupportTicketEntity> tickets = supportTicketRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, SupportTicketResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<SupportTicketResponseDTO> getSupportTicketsByStatus(String status) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching support tickets by status: doctorId: {}, status: {}", doctorId, status);
        
        List<SupportTicketEntity> tickets = supportTicketRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(doctorId, status);
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, SupportTicketResponseDTO.class))
                .collect(Collectors.toList());
    }

    private String generateUniqueTicketId() {
        String ticketId;
        do {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            ticketId = "TKT-" + timestamp.substring(timestamp.length() - 8) + "-" + random;
        } while (supportTicketRepository.existsByTicketId(ticketId));
        
        return ticketId;
    }

    private String determinePriority(String category) {
        if (category == null) {
            return "NORMAL";
        }
        
        switch (category.toLowerCase()) {
            case "technical":
            case "billing":
                return "HIGH";
            case "feature_request":
                return "LOW";
            default:
                return "NORMAL";
        }
    }

    private void sendSupportTicketEmails(SupportTicketEntity ticket) {
        try {
            String doctorSubject = "Support Ticket Created - " + ticket.getTicketId();
            String doctorBody = String.format(
                "Dear Doctor,\n\n" +
                "Your support ticket has been created successfully.\n\n" +
                "Ticket ID: %s\n" +
                "Subject: %s\n" +
                "Category: %s\n" +
                "Status: %s\n\n" +
                "We will review your request and get back to you within 24 hours.\n\n" +
                "Thank you for contacting Heal Now Support.\n\n" +
                "Best regards,\n" +
                "Heal Now Support Team",
                ticket.getTicketId(), ticket.getSubject(), ticket.getCategory(), ticket.getStatus()
            );

            emailService.sendSimpleEmail(ticket.getDoctorEmail(), doctorSubject, doctorBody);

            String adminSubject = "New Support Ticket - " + ticket.getTicketId();
            String adminBody = String.format(
                "A new support ticket has been created:\n\n" +
                "Ticket ID: %s\n" +
                "Doctor ID: %s\n" +
                "Doctor Email: %s\n" +
                "Subject: %s\n" +
                "Category: %s\n" +
                "Priority: %s\n" +
                "Message:\n%s\n\n" +
                "Created At: %s",
                ticket.getTicketId(), ticket.getDoctorId(), ticket.getDoctorEmail(),
                ticket.getSubject(), ticket.getCategory(), ticket.getPriority(),
                ticket.getMessage(), ticket.getCreatedAt().toString()
            );

            emailService.sendSimpleEmail(supportEmail, adminSubject, adminBody);

            logger.info("Support ticket emails sent successfully: ticketId: {}", ticket.getTicketId());
        } catch (Exception e) {
            logger.error("Failed to send support ticket emails: ticketId: {}, error: {}", 
                    ticket.getTicketId(), e.getMessage(), e);
        }
    }

    private void createSupportTicketNotification(SupportTicketEntity ticket) {
        try {
            String notificationTitle = "Support Ticket Created";
            String notificationMessage = String.format(
                "Your support ticket %s has been created successfully. Subject: %s. We'll respond within 24 hours.",
                ticket.getTicketId(), ticket.getSubject()
            );

            if (notificationMessage.length() > 2000) {
                notificationMessage = notificationMessage.substring(0, 1997) + "...";
            }

            NotificationEntity notification = NotificationEntity.builder()
                    .doctorId(ticket.getDoctorId())
                    .type(NotificationType.SUPPORT)
                    .title(notificationTitle)
                    .message(notificationMessage)
                    .isRead(false)
                    .build();

            notificationService.createNotificationAsync(notification).exceptionally(ex -> {
                logger.error("Failed to create support ticket notification: ticketId: {}, error: {}", 
                        ticket.getTicketId(), ex.getMessage(), ex);
                return null;
            });

            logger.info("Support ticket notification created: ticketId: {}, doctorId: {}", 
                    ticket.getTicketId(), ticket.getDoctorId());
        } catch (Exception e) {
            logger.error("Failed to create support ticket notification: ticketId: {}, error: {}", 
                    ticket.getTicketId(), e.getMessage(), e);
        }
    }
}


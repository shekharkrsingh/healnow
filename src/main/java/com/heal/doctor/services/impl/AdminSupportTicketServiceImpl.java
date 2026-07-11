package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AdminUpdateTicketDTO;
import com.heal.doctor.dto.SupportTicketResponseDTO;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.SupportTicketEntity;
import com.heal.doctor.repositories.SupportTicketRepository;
import com.heal.doctor.services.IAdminSupportTicketService;
import com.heal.doctor.services.IEmailService;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSupportTicketServiceImpl implements IAdminSupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final ModelMapper modelMapper;
    private final IEmailService emailService;
    private final INotificationService notificationService;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Override
    public List<SupportTicketResponseDTO> getAllTickets() {
        log.debug("Admin fetching all support tickets");
        List<SupportTicketEntity> tickets = supportTicketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, SupportTicketResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public Page<SupportTicketResponseDTO> getAllTicketsPaginated(Pageable pageable, String search, String status) {
        log.debug("Admin fetching paginated support tickets");
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        
        if (status != null && !status.trim().isEmpty() && !status.equals("ALL")) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("status").is(status));
        }
        
        if (search != null && !search.trim().isEmpty()) {
            String regex = ".*" + search.trim() + ".*";
            query.addCriteria(new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                org.springframework.data.mongodb.core.query.Criteria.where("ticketId").regex(regex, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("doctorEmail").regex(regex, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("subject").regex(regex, "i")
            ));
        }

        long total = mongoTemplate.count(query, SupportTicketEntity.class);
        query.with(pageable);
        List<SupportTicketEntity> tickets = mongoTemplate.find(query, SupportTicketEntity.class);

        List<SupportTicketResponseDTO> dtos = tickets.stream()
                .map(ticket -> modelMapper.map(ticket, SupportTicketResponseDTO.class))
                .collect(Collectors.toList());
                
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, total);
    }

    @Override
    @Transactional
    public SupportTicketResponseDTO updateTicketStatus(String ticketId, AdminUpdateTicketDTO updateDTO) {
        log.info("Admin updating support ticket status: ticketId: {}, status: {}", ticketId, updateDTO.getStatus());
        SupportTicketEntity ticket = supportTicketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket", ticketId));

        ticket.setStatus(updateDTO.getStatus());
        ticket.setAdminResponse(updateDTO.getAdminResponse());
        ticket.setUpdatedAt(Instant.now());
        
        // Record which admin handled it
        ticket.setAssignedTo(CurrentUserName.getCurrentUsername());

        if ("RESOLVED".equalsIgnoreCase(updateDTO.getStatus()) || "CLOSED".equalsIgnoreCase(updateDTO.getStatus())) {
            ticket.setResolvedAt(Instant.now());
        }

        SupportTicketEntity savedTicket = supportTicketRepository.save(ticket);
        
        // If status was changed, send email and notification
        if (updateDTO.getStatus() != null) {
            sendAdminResponseEmail(savedTicket);
            sendAdminResponseNotification(savedTicket);
        }

        return modelMapper.map(savedTicket, SupportTicketResponseDTO.class);
    }

    private void sendAdminResponseEmail(SupportTicketEntity ticket) {
        try {
            String subject = "Update on Support Ticket - " + ticket.getTicketId();
            String body = String.format(
                "Dear Doctor,\n\n" +
                "The status of your support ticket has been updated.\n\n" +
                "Ticket ID: %s\n" +
                "Subject: %s\n" +
                "Status: %s\n\n" +
                "Admin Response:\n%s\n\n" +
                "Thank you for contacting Heal Now Support.\n\n" +
                "Best regards,\n" +
                "Heal Now Administration",
                ticket.getTicketId(), ticket.getSubject(), ticket.getStatus(), 
                ticket.getAdminResponse() != null ? ticket.getAdminResponse() : "No additional comments."
            );

            emailService.sendSimpleEmail(ticket.getDoctorEmail(), subject, body);
            log.info("Support ticket update email sent successfully to: {}", ticket.getDoctorEmail());
        } catch (Exception e) {
            log.error("Failed to send support ticket update email to {}: {}", ticket.getDoctorEmail(), e.getMessage());
        }
    }

    private void sendAdminResponseNotification(SupportTicketEntity ticket) {
        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(ticket.getDoctorId()) // Doctor's UserID
                    .title("Support Ticket Update")
                    .message("Your support ticket (" + ticket.getTicketId() + ") status is now: " + ticket.getStatus())
                    .type(NotificationType.SUPPORT)
                    .senderId(CurrentUserName.getCurrentUsername()) // Admin who updated
                    .recipientType(NotificationRecipientType.INDIVIDUAL)
                    .link("/doctor/support-tickets/" + ticket.getTicketId())
                    .build();
            
            notificationService.createNotificationAsync(notification);
        } catch (Exception e) {
            log.error("Failed to send support ticket notification for ticket {}: {}", ticket.getTicketId(), e.getMessage());
        }
    }
}

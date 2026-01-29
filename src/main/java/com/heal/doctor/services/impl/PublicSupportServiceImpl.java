package com.heal.doctor.services.impl;

import com.heal.doctor.dto.PublicContactRequestDTO;
import com.heal.doctor.models.PublicInquiryEntity;
import com.heal.doctor.repositories.PublicInquiryRepository;
import com.heal.doctor.services.IEmailService;
import com.heal.doctor.services.IPublicSupportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PublicSupportServiceImpl implements IPublicSupportService {

    private static final Logger logger = LoggerFactory.getLogger(PublicSupportServiceImpl.class);
    private final PublicInquiryRepository publicInquiryRepository;
    private final IEmailService emailService;

    @Value("${spring.mail.username}")
    private String supportEmail;

    @Override
    public void handlePublicInquiry(PublicContactRequestDTO requestDTO) {
        logger.info("Handling public inquiry from: {}", requestDTO.getEmail());
        
        PublicInquiryEntity entity = PublicInquiryEntity.builder()
                .name(requestDTO.getName())
                .email(requestDTO.getEmail())
                .phoneNumber(requestDTO.getPhoneNumber())
                .subject(requestDTO.getSubject())
                .message(requestDTO.getMessage())
                .createdAt(Instant.now())
                .status("PENDING")
                .build();
                
        publicInquiryRepository.save(entity);
        logger.info("Public inquiry saved successfully for email: {}", requestDTO.getEmail());
        
        sendAcknowledgmentEmails(entity);
    }

    private void sendAcknowledgmentEmails(PublicInquiryEntity inquiry) {
        try {
            // 1. Send acknowledgment to the user
            String userSubject = "We've received your inquiry - Heal Now";
            String userBody = String.format(
                "Dear %s,\n\n" +
                "Thank you for reaching out to Heal Now. This is an acknowledgment that we have received your inquiry regarding '%s'.\n\n" +
                "Our team will review your message and get back to you within 24 hours.\n\n" +
                "If you have any urgent concerns, please reply to this email.\n\n" +
                "Best regards,\n" +
                "Heal Now Support Team",
                inquiry.getName(), inquiry.getSubject()
            );
            emailService.sendSimpleEmail(inquiry.getEmail(), userSubject, userBody);

            // 2. Send notification to admin/support
            String adminSubject = "New Public Inquiry: " + inquiry.getSubject();
            String adminBody = String.format(
                "A new inquiry has been submitted through the public contact form:\n\n" +
                "Name: %s\n" +
                "Email: %s\n" +
                "Phone: %s\n" +
                "Subject: %s\n" +
                "Message:\n%s\n\n" +
                "Inquiry ID: %s",
                inquiry.getName(), inquiry.getEmail(), 
                inquiry.getPhoneNumber() != null ? inquiry.getPhoneNumber() : "N/A",
                inquiry.getSubject(), inquiry.getMessage(), inquiry.getId()
            );
            emailService.sendSimpleEmail(supportEmail, adminSubject, adminBody);
            
            logger.info("Acknowledgment emails sent for inquiry: {}", inquiry.getId());
        } catch (Exception e) {
            logger.error("Failed to send acknowledgment emails for inquiry: {}, error: {}", 
                    inquiry.getId(), e.getMessage(), e);
        }
    }
}

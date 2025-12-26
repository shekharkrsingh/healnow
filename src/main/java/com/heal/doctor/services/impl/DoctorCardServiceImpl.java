package com.heal.doctor.services.impl;

import com.heal.doctor.exception.DoctorCardGenerationException;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.IDoctorCardService;
import com.heal.doctor.services.IEmailService;
import com.heal.doctor.services.IPdfService;
import com.heal.doctor.services.IQRCodeService;
import com.heal.doctor.utils.CurrentUserName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class DoctorCardServiceImpl implements IDoctorCardService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorCardServiceImpl.class);
    private static final String CARD_TEMPLATE_NAME = "appointment-booking-qr-card";
    private static final String EMAIL_SUBJECT = "Your Digital Appointment Booking Card - H-Potion";
    private static final String PDF_FILENAME = "appointment-booking-card.pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final TemplateEngine templateEngine;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final IQRCodeService qrCodeService;
    private final IEmailService emailService;
    private final IPdfService pdfService;

    @Value("${app.booking.base-url}")
    private String bookingBaseUrl;

    @Value("${app.company.name:H-Potion}")
    private String companyName;

    @Autowired
    public DoctorCardServiceImpl(
            TemplateEngine templateEngine,
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            IQRCodeService qrCodeService,
            IEmailService emailService,
            IPdfService pdfService) {
        this.templateEngine = templateEngine;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.emailService = emailService;
        this.pdfService = pdfService;
    }

    @Override
    public byte[] generateDoctorCard() {
        String doctorId= CurrentUserName.getCurrentUserId();
        try {
            DoctorEntity doctor = getDoctorEntity(doctorId);
            String bookingUrl= buildDefaultBookingUrl(doctorId);
            String qrCodeDataUrl = qrCodeService.generateQRCodeDataUrl(bookingUrl);

            Map<String, Object> variables = buildTemplateVariables(doctor, qrCodeDataUrl);
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(CARD_TEMPLATE_NAME, context);

            byte[] pdfBytes = pdfService.generatePdfFromHtml(htmlContent);

            // Send email asynchronously
            sendDoctorCardByEmail(doctorId, pdfBytes);

            return pdfBytes;

        } catch (Exception e) {
            logger.error("Failed to generate doctor card for doctorId: {}", doctorId, e);
            throw new DoctorCardGenerationException("Failed to generate doctor card for doctorId: " + doctorId, e);
        }
    }

    @Override
    public CompletableFuture<Void> sendDoctorCardByEmail(String doctorId, byte[] pdfBytes) {
        try {
            DoctorEntity doctor = getDoctorEntity(doctorId);
            String doctorEmail = getDoctorEmail(doctor);

            return emailService.sendSimpleEmailWithAttachment(
                    doctorEmail,
                    EMAIL_SUBJECT,
                    buildEmailBody(doctor),
                    pdfBytes,
                    PDF_FILENAME,
                    PDF_CONTENT_TYPE
            );

        } catch (Exception e) {
            logger.error("Failed to send doctor card email for doctorId: {}", doctorId, e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new DoctorCardGenerationException(
                    "Failed to send doctor card email for doctorId: " + doctorId, e));
            return future;
        }
    }

    @Override
    public CompletableFuture<Void> generateAndSendDoctorCard(String doctorId) {
        byte[] pdfBytes = generateDoctorCard();
        return sendDoctorCardByEmail(doctorId, pdfBytes);
    }

    private DoctorEntity getDoctorEntity(String doctorId) {
        return doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new DoctorCardGenerationException("Doctor not found with doctorId: " + doctorId));
    }

    private String getDoctorEmail(DoctorEntity doctor) {
        return userRepository.findByUserId(doctor.getDoctorId())
                .map(UserEntity::getEmail)
                .orElseThrow(() -> new DoctorCardGenerationException(
                        "User account not found for doctorId: " + doctor.getDoctorId()));
    }

    private String buildDefaultBookingUrl(String doctorId) {
        return bookingBaseUrl + "/" + doctorId;
    }

    private Map<String, Object> buildTemplateVariables(DoctorEntity doctor, String qrCodeDataUrl) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("companyName", companyName);
        variables.put("qrCodeData", qrCodeDataUrl);
        variables.put("clinicName", doctor.getClinicAddress() != null && !doctor.getClinicAddress().isEmpty()
                ? "Clinic"
                : getDoctorFullName(doctor) + "'s Clinic");
        variables.put("clinicAddress", doctor.getClinicAddress() != null ? doctor.getClinicAddress() : "N/A");
        variables.put("phoneNumber", formatPhoneNumber(doctor.getPhoneNumber()));
        variables.put("clinicInitial", getClinicInitial(variables.get("clinicName").toString()));
        variables.put("doctorName", getDoctorFullName(doctor));
        variables.put("specialization", doctor.getSpecialization() != null ? doctor.getSpecialization() : "Medical Professional");
        variables.put("doctorId", doctor.getDoctorId());
        variables.put("cardId", generateCardId());
        variables.put("generationDate", LocalDateTime.now().format(DATE_FORMATTER));

        return variables;
    }

    private String getClinicInitial(String clinicName) {
        if (clinicName == null || clinicName.trim().isEmpty()) {
            return "HC";
        }
        String[] words = clinicName.trim().split("\\s+");
        if (words.length >= 2) {
            return (words[0].charAt(0) + words[1].substring(0, 1)).toUpperCase();
        }
        return clinicName.substring(0, Math.min(2, clinicName.length())).toUpperCase();
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() != 10) {
            return phoneNumber;
        }
        return String.format("(%s) %s-%s",
                phoneNumber.substring(0, 3),
                phoneNumber.substring(3, 6),
                phoneNumber.substring(6));
    }

    private String generateCardId() {
        return "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String getDoctorFullName(DoctorEntity doctor) {
        return "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
    }

    private String buildEmailBody(DoctorEntity doctor) {
        return String.format(
                """
                        Dear %s,
                        
                        Please find attached your digital appointment booking card.
                        
                        This card contains a QR code that patients can scan to book appointments with you directly.
                        
                        You can print this card and display it in your clinic, or share it digitally with your patients.
                        
                        Best regards,
                        %s Team""",
                getDoctorFullName(doctor),
                companyName
        );
    }
}
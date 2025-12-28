package com.heal.doctor.services.impl;

import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.services.IAppointmentConfirmationService;
import com.heal.doctor.services.IEmailService;
import com.heal.doctor.services.IPdfService;
import com.heal.doctor.services.IQRCodeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AppointmentConfirmationServiceImpl implements IAppointmentConfirmationService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentConfirmationServiceImpl.class);
    private static final String TEMPLATE_NAME = "appointment-booking-confirmation-template";
    private static final String ATTACHMENT_FILENAME = "appointment-confirmation.pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final TemplateEngine templateEngine;
    private final DoctorRepository doctorRepository;
    private final IQRCodeService qrCodeService;
    private final IPdfService pdfService;
    private final IEmailService emailService;

    @Value("${app.appointment.confirmation-url}")
    private String confirmationBaseUrl;

    @Value("${company.name}")
    private String companyName;

    @Override
    public CompletableFuture<Void> sendConfirmationEmail(AppointmentEntity appointment) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Generating appointment confirmation for appointmentId: {}", appointment.getAppointmentId());

                DoctorEntity doctor = doctorRepository.findByDoctorId(appointment.getDoctorId())
                        .orElseThrow(() -> new RuntimeException("Doctor not found: " + appointment.getDoctorId()));

                String confirmationUrl = confirmationBaseUrl +  appointment.getAppointmentId();
                byte[] qrCodeBytes = qrCodeService.generateQRCodeBytes(confirmationUrl, 300, 300);
                String qrCodeDataUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(qrCodeBytes);

                Context context = new Context();
                Map<String, Object> variables = new HashMap<>();
                
                // Clinic Info
                String clinicName = doctor.getClinicName() != null && !doctor.getClinicName().isEmpty() 
                                    ? doctor.getClinicName() 
                                    : "Dr. " + doctor.getFirstName() + " " + doctor.getLastName() + "'s Clinic";
                
                variables.put("clinicName", clinicName);
                variables.put("clinicInitial", getClinicInitial(clinicName));
                variables.put("companyTagline", "Excellence in Healthcare Services");
                variables.put("clinicAddress", doctor.getClinicAddress() != null ? doctor.getClinicAddress() : "N/A");
                variables.put("clinicContactNumber", doctor.getClinicContactNumber() != null ? formatPhoneNumber(doctor.getClinicContactNumber()) : "N/A");
                variables.put("clinicEmail", doctor.getClinicEmail() != null ? doctor.getClinicEmail() : "N/A");

                // Appointment Info
                variables.put("appointmentId", appointment.getAppointmentId());
                variables.put("documentId", "CONF-" + appointment.getAppointmentId());
                variables.put("appointmentDateTime", appointment.getAppointmentDateTime());
                variables.put("generationDate", new Date());
                variables.put("appointmentType", appointment.getAppointmentType() != null ? appointment.getAppointmentType().name() : "IN_PERSON");
                variables.put("paymentStatus", appointment.getPaymentStatus() != null ? appointment.getPaymentStatus() : false);
                variables.put("isEmergency", appointment.getIsEmergency() != null ? appointment.getIsEmergency() : false);

                // Patient Info
                variables.put("patientName", appointment.getPatientName());
                variables.put("contact", formatPhoneNumber(appointment.getContact()));
                variables.put("email", appointment.getEmail());

                // Doctor Info
                variables.put("doctorName", "Dr. " + doctor.getFirstName() + " " + doctor.getLastName());
                variables.put("specialization", doctor.getSpecialization() != null ? doctor.getSpecialization() : "General Medicine");

                // QR Code
                variables.put("qrCodeData", qrCodeDataUrl);

                context.setVariables(variables);

                String htmlContent = templateEngine.process(TEMPLATE_NAME, context);
                byte[] pdfBytes = pdfService.generatePdfFromHtml(htmlContent);

                // Update variable for Email Body (CID)
                variables.put("qrCodeData", "cid:qrCodeImage");

                emailService.sendHtmlEmailWithAttachment(
                        appointment.getEmail(),
                        "Appointment Confirmation - " + clinicName,
                        TEMPLATE_NAME,
                        variables,
                        pdfBytes,
                        ATTACHMENT_FILENAME,
                        PDF_CONTENT_TYPE,
                        Map.of("qrCodeImage", qrCodeBytes)
                ).join();

                logger.info("Confirmation email sent successfully for appointmentId: {}", appointment.getAppointmentId());

            } catch (Exception e) {
                logger.error("Failed to send appointment confirmation email for appointmentId: {}", appointment.getAppointmentId(), e);
                throw new RuntimeException("Failed to send confirmation email", e);
            }
        });
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
}

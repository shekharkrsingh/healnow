package com.heal.doctor.scheduler;

import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.VerificationStatus;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.services.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DoctorLicenseExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DoctorLicenseExpiryScheduler.class);

    private final DoctorRepository doctorRepository;
    private final INotificationService notificationService;
    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final String companyName;

    public DoctorLicenseExpiryScheduler(
            DoctorRepository doctorRepository, 
            INotificationService notificationService,
            UserRepository userRepository,
            IEmailService emailService,
            @Value("${company.name}") String companyName) {
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.companyName = companyName;
    }

    // Runs daily at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkLicenseExpiry() {
        logger.info("Starting scheduled doctor license expiry check");

        try {
            Date now = new Date();
            List<DoctorEntity> expiredVerifiedDoctors = doctorRepository
                    .findByLicenseExpiryDateBeforeAndVerificationStatus(now, VerificationStatus.VERIFIED);

            logger.info("Found {} verified doctors with expired licenses", expiredVerifiedDoctors.size());

            for (DoctorEntity doctor : expiredVerifiedDoctors) {
                logger.warn("Suspending doctor due to expired license: doctorId: {}, expiryDate: {}", 
                        doctor.getDoctorId(), doctor.getLicenseExpiryDate());

                doctor.setVerificationStatus(VerificationStatus.SUSPENDED);
                doctor.setUpdatedAt(now);
                doctorRepository.save(doctor);

                // Create a system notification to inform the doctor
                NotificationEntity notification = NotificationEntity.builder()
                        .targetId(doctor.getDoctorId())
                        .recipientType(NotificationRecipientType.INDIVIDUAL)
                        .type(NotificationType.SYSTEM)
                        .title("Practice Account Suspended")
                        .message("Your medical license expired on " + doctor.getLicenseExpiryDate() + 
                                 ". Your verified status has been suspended. Please update your licensing details to request re-verification.")
                        .createdAt(now.toInstant())
                        .build();

                notificationService.createNotificationAsync(notification).exceptionally(ex -> {
                    logger.error("Failed to create license expiry suspension notification asynchronously for doctorId: {}, error: {}", 
                            doctor.getDoctorId(), ex.getMessage(), ex);
                    return null;
                });

                // Send email notification to doctor
                try {
                    String doctorEmail = doctor.getClinicEmail(); // Fallback to clinic email
                    Optional<UserEntity> userOpt = userRepository.findByUserId(doctor.getDoctorId());
                    if (userOpt.isPresent()) {
                        doctorEmail = userOpt.get().getEmail();
                    }

                    if (doctorEmail != null && !doctorEmail.isEmpty()) {
                        final String toEmail = doctorEmail;
                        final String doctorName = doctor.getFirstName() + " " + doctor.getLastName();
                        final String licenseNumber = doctor.getLicenseNumber() != null ? doctor.getLicenseNumber() : "N/A";
                        final String expiryDateStr = doctor.getLicenseExpiryDate() != null ? doctor.getLicenseExpiryDate().toString() : "N/A";

                        emailService.sendHtmlEmail(
                                toEmail,
                                "Practice Account Suspended - " + companyName,
                                "license-suspended.template.html",
                                Map.of(
                                        "companyName", companyName,
                                        "doctorName", doctorName,
                                        "licenseNumber", licenseNumber,
                                        "expiryDate", expiryDateStr
                                )
                        ).exceptionally(ex -> {
                            logger.error("Failed to send license suspension email to doctorId: {}, email: {}, error: {}", 
                                    doctor.getDoctorId(), toEmail, ex.getMessage());
                            return null;
                        });
                    }
                } catch (Exception ex) {
                    logger.error("Error setting up license suspension email for doctorId: {}", doctor.getDoctorId(), ex);
                }
            }

            // Check for pre-expiry warning notifications (30 days and 7 days)
            sendPreExpiryWarnings(30);
            sendPreExpiryWarnings(7);

            logger.info("Scheduled doctor license expiry check completed successfully");
        } catch (Exception e) {
            logger.error("Error occurred during scheduled doctor license expiry check", e);
        }
    }

    private void sendPreExpiryWarnings(int daysRemaining) {
        logger.info("Checking doctors with licenses expiring in {} days", daysRemaining);
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, daysRemaining);
            
            // Start of target day
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date start = cal.getTime();
            
            // End of target day
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            Date end = cal.getTime();

            List<DoctorEntity> doctorsWarning = doctorRepository
                    .findByLicenseExpiryDateBetweenAndVerificationStatus(start, end, VerificationStatus.VERIFIED);

            logger.info("Found {} doctors whose license expires in {} days", doctorsWarning.size(), daysRemaining);

            for (DoctorEntity doctor : doctorsWarning) {
                // Send warning in-app notification
                NotificationEntity notification = NotificationEntity.builder()
                        .targetId(doctor.getDoctorId())
                        .recipientType(NotificationRecipientType.INDIVIDUAL)
                        .type(NotificationType.SYSTEM)
                        .title("Medical License Expiring Soon")
                        .message("Your medical license expires in " + daysRemaining + " days (on " + doctor.getLicenseExpiryDate() + 
                                 "). Please update your licensing details as soon as possible to avoid automatic suspension of practice bookings.")
                        .createdAt(java.time.Instant.now())
                        .build();

                notificationService.createNotificationAsync(notification).exceptionally(ex -> {
                    logger.error("Failed to create pre-expiry notification asynchronously for doctorId: {}, error: {}", 
                            doctor.getDoctorId(), ex.getMessage(), ex);
                    return null;
                });

                // Send warning email
                try {
                    String doctorEmail = doctor.getClinicEmail();
                    Optional<UserEntity> userOpt = userRepository.findByUserId(doctor.getDoctorId());
                    if (userOpt.isPresent()) {
                        doctorEmail = userOpt.get().getEmail();
                    }

                    if (doctorEmail != null && !doctorEmail.isEmpty()) {
                        final String toEmail = doctorEmail;
                        final String doctorName = doctor.getFirstName() + " " + doctor.getLastName();
                        final String licenseNumber = doctor.getLicenseNumber() != null ? doctor.getLicenseNumber() : "N/A";
                        final String expiryDateStr = doctor.getLicenseExpiryDate() != null ? doctor.getLicenseExpiryDate().toString() : "N/A";

                        emailService.sendHtmlEmail(
                                toEmail,
                                "Medical License Expiring Soon - " + companyName,
                                "license-expiring-warning.template.html",
                                Map.of(
                                        "companyName", companyName,
                                        "doctorName", doctorName,
                                        "licenseNumber", licenseNumber,
                                        "expiryDate", expiryDateStr,
                                        "daysRemaining", daysRemaining
                                )
                        ).exceptionally(ex -> {
                            logger.error("Failed to send pre-expiry warning email to doctorId: {}, email: {}, error: {}", 
                                    doctor.getDoctorId(), toEmail, ex.getMessage());
                            return null;
                        });
                    }
                } catch (Exception ex) {
                    logger.error("Error setting up pre-expiry warning email for doctorId: {}", doctor.getDoctorId(), ex);
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred during pre-expiry check for {} days", daysRemaining, e);
        }
    }
}

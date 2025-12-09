package com.heal.doctor.scheduler;

import com.heal.doctor.repositories.NotificationRepository;
import com.heal.doctor.repositories.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class CleanupSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(CleanupSchedulerService.class);
    
    private final NotificationRepository notificationRepository;
    private final OtpRepository otpRepository;

    public CleanupSchedulerService(NotificationRepository notificationRepository, OtpRepository otpRepository) {
        this.notificationRepository = notificationRepository;
        this.otpRepository = otpRepository;
    }

    /**
     * Scheduled task to run at midnight (00:00:00) every day
     * Deletes notifications older than 10 days and OTPs older than 1 day
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldData() {
        logger.info("Starting scheduled cleanup task at midnight");
        
        try {
            // Delete notifications older than 10 days
            Instant notificationCutoffDate = Instant.now().minus(10, ChronoUnit.DAYS);
            long notificationCountBefore = notificationRepository.count();
            notificationRepository.deleteByCreatedAtBefore(notificationCutoffDate);
            long notificationCountAfter = notificationRepository.count();
            long deletedNotifications = notificationCountBefore - notificationCountAfter;
            logger.info("Deleted {} notifications older than 10 days", deletedNotifications);

            // Delete OTPs older than 1 day
            Date otpCutoffDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
            long otpCountBefore = otpRepository.count();
            otpRepository.deleteByCreatedAtBefore(otpCutoffDate);
            long otpCountAfter = otpRepository.count();
            long deletedOtps = otpCountBefore - otpCountAfter;
            logger.info("Deleted {} OTPs older than 1 day", deletedOtps);

            logger.info("Scheduled cleanup task completed successfully");
        } catch (Exception e) {
            logger.error("Error occurred during scheduled cleanup task", e);
        }
    }
}

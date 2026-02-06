package com.heal.doctor.scheduler.impl;

import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.repositories.AppointmentRepository;
import com.heal.doctor.scheduler.IAppointmentStatusScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class AppointmentStatusSchedulerImpl implements IAppointmentStatusScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentStatusSchedulerImpl.class);

    private final AppointmentRepository appointmentRepository;

    public AppointmentStatusSchedulerImpl(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Daily job at 3:00 AM to mark past appointments as MISSED.
     * Criteria:
     * - Appointment date is strictly before today (00:00:00).
     * - Status is ACCEPTED or REACTIVATED.
     * - Not TREATED (implied by status check, but logic reinforces this).
     */
    @Scheduled(cron = "0 0 3 * * ?") 
    @Transactional
    public void markMissedAppointments() {
        logger.info("Starting scheduled task: Mark Missed Appointments");

        try {
            // Get start of today (midnight)
            LocalDate today = LocalDate.now();
            Date startOfToday = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<AppointmentStatus> targetStatuses = Arrays.asList(
                    AppointmentStatus.ACCEPTED,
                    AppointmentStatus.REACTIVATED
            );

            // Fetch appointments that should be missed (before today)
            List<AppointmentEntity> missedAppointments = appointmentRepository
                    .findByAppointmentDateTimeBeforeAndStatusIn(startOfToday, targetStatuses);

            if (missedAppointments.isEmpty()) {
                logger.info("No appointments found to mark as MISSED.");
                return;
            }

            logger.info("Found {} appointments to mark as MISSED.", missedAppointments.size());

            for (AppointmentEntity appointment : missedAppointments) {
                logger.info("Marking appointment {} as MISSED (Old Status: {}, Date: {})",
                        appointment.getAppointmentId(), appointment.getStatus(), appointment.getAppointmentDateTime());
                
                appointment.setStatus(AppointmentStatus.MISSED);
            }

            appointmentRepository.saveAll(missedAppointments);
            logger.info("Successfully marked {} appointments as MISSED.", missedAppointments.size());

        } catch (Exception e) {
            logger.error("Error occurred while marking missed appointments", e);
        }
    }
}

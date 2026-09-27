package com.heal.doctor.scheduler.impl;

import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.repositories.AppointmentRepository;
import com.heal.doctor.scheduler.IAppointmentStatusScheduler;
import com.heal.doctor.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class AppointmentStatusSchedulerImpl implements IAppointmentStatusScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentStatusSchedulerImpl.class);

    private final AppointmentRepository appointmentRepository;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public AppointmentStatusSchedulerImpl(AppointmentRepository appointmentRepository, org.springframework.data.mongodb.core.MongoTemplate mongoTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Daily job at 3:00 AM IST to mark past appointments as MISSED.
     * The cron zone is explicitly set to Asia/Kolkata so the job always fires at
     * 3 AM IST regardless of the JVM / server timezone (e.g. UTC on cloud).
     *
     * Logic:
     * 1. Fetch only appointmentIds of expired appointments (Optimization).
     * 2. Perform bulk update on these IDs.
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Kolkata")
    @Transactional
    public void markMissedAppointments() {
        logger.info("Starting scheduled task: Mark Missed Appointments");

        try {
            // Use IST date so "today" is correct even when the server runs in UTC.
            LocalDate today = LocalDate.now(DateUtils.IST);
            // Start of today in IST → midnight 00:00:00 IST as UTC epoch millis.
            Date startOfToday = Date.from(today.atStartOfDay(DateUtils.IST).toInstant());

            List<AppointmentStatus> targetStatuses = Arrays.asList(
                    AppointmentStatus.ACCEPTED,
                    AppointmentStatus.REACTIVATED
            );

            // 1. Fetch only IDs (Projection)
            List<AppointmentEntity> expiredDocs = appointmentRepository
                    .findExpiredAppointmentIds(startOfToday, targetStatuses);

            if (expiredDocs.isEmpty()) {
                logger.info("No appointments found to mark as MISSED.");
                return;
            }

            List<String> appointmentIds = expiredDocs.stream()
                    .map(AppointmentEntity::getAppointmentId)
                    .toList();

            logger.info("Found {} appointments to mark as MISSED: {}", appointmentIds.size(), appointmentIds);

            // 2. Bulk Update
            org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
                    org.springframework.data.mongodb.core.query.Criteria.where("appointmentId").in(appointmentIds)
            );
            org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                    .set("status", AppointmentStatus.MISSED);

            mongoTemplate.updateMulti(query, update, AppointmentEntity.class);

            logger.info("Successfully marked {} appointments as MISSED via bulk update.", appointmentIds.size());

        } catch (Exception e) {
            logger.error("Error occurred while marking missed appointments", e);
        }
    }
}


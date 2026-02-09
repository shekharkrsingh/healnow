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
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public AppointmentStatusSchedulerImpl(AppointmentRepository appointmentRepository, org.springframework.data.mongodb.core.MongoTemplate mongoTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Daily job at 3:00 AM to mark past appointments as MISSED.
     * Logic:
     * 1. Fetch only appointmentIds of expired appointments (Optimization).
     * 2. Perform bulk update on these IDs.
     */
    @Scheduled(cron = "0 0 3 * * ?") 
    @Transactional
    public void markMissedAppointments() {
        logger.info("Starting scheduled task: Mark Missed Appointments");

        try {
            LocalDate today = LocalDate.now();
            Date startOfToday = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

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

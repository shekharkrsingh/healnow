package com.heal.doctor.services.impl;

import com.heal.doctor.analytics.models.AnalyticsEntity;
import com.heal.doctor.analytics.repositories.AnalyticsRepository;
import com.heal.doctor.dto.AdminDashboardDTO;
import com.heal.doctor.dto.AdminDashboardDTO.DailyTrendPoint;
import com.heal.doctor.models.*;
import com.heal.doctor.models.enums.CollaboratorStatus;
import com.heal.doctor.models.enums.VerificationStatus;
import com.heal.doctor.repositories.*;
import com.heal.doctor.services.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final AnalyticsRepository analyticsRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public AdminDashboardDTO getDashboardAnalytics() {
        log.info("Building admin dashboard analytics");

        // ---- DOCTORS ----
        List<DoctorEntity> doctors = doctorRepository.findAll();

        Map<String, Long> doctorsByVerificationStatus = doctors.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getVerificationStatus() != null ? d.getVerificationStatus().name() : "UNKNOWN",
                        Collectors.counting()));

        Map<String, Long> doctorsBySpecialization = doctors.stream()
                .filter(d -> d.getSpecialization() != null && !d.getSpecialization().isBlank())
                .collect(Collectors.groupingBy(DoctorEntity::getSpecialization, Collectors.counting()));

        Map<String, Long> doctorsByGender = doctors.stream()
                .filter(d -> d.getGender() != null)
                .collect(Collectors.groupingBy(d -> d.getGender().name(), Collectors.counting()));

        Map<String, Long> doctorsByCity = doctors.stream()
                .filter(d -> d.getAddress() != null && d.getAddress().getCity() != null && !d.getAddress().getCity().isBlank())
                .collect(Collectors.groupingBy(d -> d.getAddress().getCity(), Collectors.counting()));

        Date thirtyDaysFromNow = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));
        List<DoctorEntity> expiringLicenses = doctorRepository
                .findByLicenseExpiryDateBetweenAndVerificationStatus(new Date(), thirtyDaysFromNow, VerificationStatus.VERIFIED);

        List<DailyTrendPoint> doctorTrend = buildTrend(doctors.stream()
                .filter(d -> d.getCreatedAt() != null)
                .map(d -> d.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toList()), 365);

        // ---- APPOINTMENTS ----
        List<AppointmentEntity> appointments = appointmentRepository.findAll();

        Map<String, Long> appointmentsByStatus = appointments.stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));

        Map<String, Long> appointmentsByType = appointments.stream()
                .filter(a -> a.getAppointmentType() != null)
                .collect(Collectors.groupingBy(a -> a.getAppointmentType().name(), Collectors.counting()));

        long treatedCount = appointments.stream().filter(a -> Boolean.TRUE.equals(a.getTreated())).count();
        long emergencyCount = appointments.stream().filter(a -> Boolean.TRUE.equals(a.getIsEmergency())).count();
        long paidCount = appointments.stream().filter(a -> Boolean.TRUE.equals(a.getPaymentStatus())).count();

        List<DailyTrendPoint> appointmentTrend = buildTrend(appointments.stream()
                .filter(a -> a.getBookingDateTime() != null)
                .map(a -> a.getBookingDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toList()), 365);

        // ---- SUPPORT TICKETS ----
        List<SupportTicketEntity> tickets = supportTicketRepository.findAll();

        Map<String, Long> ticketsByStatus = tickets.stream()
                .filter(t -> t.getStatus() != null)
                .collect(Collectors.groupingBy(SupportTicketEntity::getStatus, Collectors.counting()));

        Map<String, Long> ticketsByPriority = tickets.stream()
                .filter(t -> t.getPriority() != null)
                .collect(Collectors.groupingBy(SupportTicketEntity::getPriority, Collectors.counting()));

        Map<String, Long> ticketsByCategory = tickets.stream()
                .filter(t -> t.getCategory() != null && !t.getCategory().isBlank())
                .collect(Collectors.groupingBy(SupportTicketEntity::getCategory, Collectors.counting()));

        List<DailyTrendPoint> ticketTrend = buildTrend(tickets.stream()
                .filter(t -> t.getCreatedAt() != null)
                .map(t -> t.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toList()), 365);

        // ---- COLLABORATORS ----
        List<CollaboratorProfileEntity> collaborators = collaboratorProfileRepository.findAll();

        Map<String, Long> collaboratorsByStatus = collaborators.stream()
                .filter(c -> c.getStatus() != null)
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));

        List<DailyTrendPoint> collaboratorTrend = buildTrend(collaborators.stream()
                .filter(c -> c.getCreatedAt() != null)
                .map(c -> c.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toList()), 365);

        // ---- ANALYTICS ----
        List<AnalyticsEntity> analyticsEvents = analyticsRepository.findAll();

        Map<String, Long> byPlatform = analyticsEvents.stream()
                .filter(a -> a.getPlatform() != null)
                .collect(Collectors.groupingBy(AnalyticsEntity::getPlatform, Collectors.counting()));

        Map<String, Long> byCity = analyticsEvents.stream()
                .filter(a -> a.getCity() != null && !a.getCity().isBlank())
                .collect(Collectors.groupingBy(AnalyticsEntity::getCity, Collectors.counting()));

        Map<String, Long> byAppVersion = analyticsEvents.stream()
                .filter(a -> a.getAppVersion() != null)
                .collect(Collectors.groupingBy(AnalyticsEntity::getAppVersion, Collectors.counting()));

        Map<String, Long> byNetwork = analyticsEvents.stream()
                .filter(a -> a.getNetworkType() != null)
                .collect(Collectors.groupingBy(AnalyticsEntity::getNetworkType, Collectors.counting()));

        // ---- NOTIFICATIONS ----
        List<NotificationEntity> notifications = notificationRepository.findAll();

        Map<String, Long> notificationsByType = notifications.stream()
                .filter(n -> n.getType() != null)
                .collect(Collectors.groupingBy(n -> n.getType().name(), Collectors.counting()));

        return AdminDashboardDTO.builder()
                .totalDoctors(doctors.size())
                .verifiedDoctors((int) doctors.stream().filter(d -> d.getVerificationStatus() == VerificationStatus.VERIFIED).count())
                .pendingVerificationDoctors((int) doctors.stream().filter(d -> d.getVerificationStatus() == VerificationStatus.PENDING).count())
                .rejectedDoctors((int) doctors.stream().filter(d -> d.getVerificationStatus() == VerificationStatus.REJECTED).count())
                .suspendedDoctors((int) doctors.stream().filter(d -> d.getVerificationStatus() == VerificationStatus.SUSPENDED).count())
                .terminatedDoctors((int) doctors.stream().filter(d -> d.getVerificationStatus() == VerificationStatus.TERMINATED).count())
                .doctorsWithExpiringLicenses(expiringLicenses.size())
                .doctorsBySpecialization(doctorsBySpecialization)
                .doctorsByVerificationStatus(doctorsByVerificationStatus)
                .doctorsByGender(doctorsByGender)
                .doctorsByCity(doctorsByCity)
                .doctorGrowthTrend(doctorTrend)
                .totalAppointments(appointments.size())
                .bookedAppointments(appointmentsByStatus.getOrDefault("BOOKED", 0L))
                .acceptedAppointments(appointmentsByStatus.getOrDefault("ACCEPTED", 0L))
                .cancelledAppointments(appointmentsByStatus.getOrDefault("CANCELLED", 0L))
                .missedAppointments(appointmentsByStatus.getOrDefault("MISSED", 0L))
                .reactivatedAppointments(appointmentsByStatus.getOrDefault("REACTIVATED", 0L))
                .treatedAppointments(treatedCount)
                .emergencyAppointments(emergencyCount)
                .inPersonAppointments(appointmentsByType.getOrDefault("IN_PERSON", 0L))
                .onlineAppointments(appointmentsByType.getOrDefault("ONLINE", 0L))
                .paidAppointments(paidCount)
                .appointmentsByStatus(appointmentsByStatus)
                .appointmentsByType(appointmentsByType)
                .appointmentTrend(appointmentTrend)
                .totalSupportTickets(tickets.size())
                .openTickets(ticketsByStatus.getOrDefault("OPEN", 0L))
                .inProgressTickets(ticketsByStatus.getOrDefault("IN_PROGRESS", 0L))
                .resolvedTickets(ticketsByStatus.getOrDefault("RESOLVED", 0L))
                .closedTickets(ticketsByStatus.getOrDefault("CLOSED", 0L))
                .ticketsByStatus(ticketsByStatus)
                .ticketsByPriority(ticketsByPriority)
                .ticketsByCategory(ticketsByCategory)
                .ticketTrend(ticketTrend)
                .totalCollaborators(collaborators.size())
                .activeCollaborators((int) collaborators.stream().filter(c -> c.getStatus() == CollaboratorStatus.ACTIVATED).count())
                .invitedCollaborators((int) collaborators.stream().filter(c -> c.getStatus() == CollaboratorStatus.INVITED).count())
                .deactivatedCollaborators((int) collaborators.stream().filter(c -> c.getStatus() == CollaboratorStatus.DEACTIVATED).count())
                .collaboratorsByStatus(collaboratorsByStatus)
                .collaboratorGrowthTrend(collaboratorTrend)
                .totalAnalyticsEvents((long) analyticsEvents.size())
                .analyticsUsersByPlatform(byPlatform)
                .analyticsUsersByCity(byCity)
                .analyticsUsersByAppVersion(byAppVersion)
                .analyticsUsersByNetworkType(byNetwork)
                .totalNotifications((long) notifications.size())
                .notificationsByType(notificationsByType)
                .build();
    }

    private List<DailyTrendPoint> buildTrend(List<LocalDate> dates, int daysBack) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(daysBack - 1L);

        Map<String, Long> countByDate = dates.stream()
                .filter(d -> !d.isBefore(start) && !d.isAfter(today))
                .collect(Collectors.groupingBy(d -> d.format(DATE_FMT), Collectors.counting()));

        return Stream.iterate(start, d -> d.plusDays(1))
                .limit(daysBack)
                .map(d -> new DailyTrendPoint(d.format(DATE_FMT), countByDate.getOrDefault(d.format(DATE_FMT), 0L)))
                .collect(Collectors.toList());
    }
}

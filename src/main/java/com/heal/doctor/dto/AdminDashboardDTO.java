package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDTO {

    // === DOCTOR METRICS ===
    private int totalDoctors;
    private int verifiedDoctors;
    private int pendingVerificationDoctors;
    private int rejectedDoctors;
    private int suspendedDoctors;
    private int terminatedDoctors;
    private int doctorsWithExpiringLicenses;
    private Map<String, Long> doctorsBySpecialization;
    private Map<String, Long> doctorsByVerificationStatus;
    private Map<String, Long> doctorsByGender;
    private Map<String, Long> doctorsByCity;
    private List<DailyTrendPoint> doctorGrowthTrend;

    // === APPOINTMENT METRICS ===
    private long totalAppointments;
    private long bookedAppointments;
    private long acceptedAppointments;
    private long cancelledAppointments;
    private long missedAppointments;
    private long reactivatedAppointments;
    private long treatedAppointments;
    private long emergencyAppointments;
    private long inPersonAppointments;
    private long onlineAppointments;
    private long paidAppointments;
    private Map<String, Long> appointmentsByStatus;
    private Map<String, Long> appointmentsByType;
    private List<DailyTrendPoint> appointmentTrend;

    // === SUPPORT TICKET METRICS ===
    private long totalSupportTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private Map<String, Long> ticketsByStatus;
    private Map<String, Long> ticketsByPriority;
    private Map<String, Long> ticketsByCategory;
    private List<DailyTrendPoint> ticketTrend;

    // === COLLABORATOR METRICS ===
    private int totalCollaborators;
    private int activeCollaborators;
    private int invitedCollaborators;
    private int deactivatedCollaborators;
    private Map<String, Long> collaboratorsByStatus;
    private List<DailyTrendPoint> collaboratorGrowthTrend;

    // === ANALYTICS / DEVICE METRICS ===
    private long totalAnalyticsEvents;
    private Map<String, Long> analyticsUsersByPlatform;
    private Map<String, Long> analyticsUsersByCity;
    private Map<String, Long> analyticsUsersByAppVersion;
    private Map<String, Long> analyticsUsersByNetworkType;

    // === NOTIFICATION METRICS ===
    private long totalNotifications;
    private Map<String, Long> notificationsByType;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyTrendPoint {
        private String date;
        private long count;
    }
}

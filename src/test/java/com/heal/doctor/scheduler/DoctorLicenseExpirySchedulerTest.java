package com.heal.doctor.scheduler;

import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.VerificationStatus;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.services.IEmailService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DoctorLicenseExpirySchedulerTest {

    private DoctorRepository doctorRepository;
    private INotificationService notificationService;
    private UserRepository userRepository;
    private IEmailService emailService;
    private DoctorLicenseExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        doctorRepository = mock(DoctorRepository.class);
        notificationService = mock(INotificationService.class);
        userRepository = mock(UserRepository.class);
        emailService = mock(IEmailService.class);
        scheduler = new DoctorLicenseExpiryScheduler(doctorRepository, notificationService, userRepository, emailService, "Heal Now");
    }

    @Test
    void testCheckLicenseExpiry_NoExpiredDoctors() {
        when(doctorRepository.findByLicenseExpiryDateBeforeAndVerificationStatus(any(Date.class), eq(VerificationStatus.VERIFIED)))
                .thenReturn(Collections.emptyList());

        scheduler.checkLicenseExpiry();

        verify(doctorRepository, times(1)).findByLicenseExpiryDateBeforeAndVerificationStatus(any(Date.class), eq(VerificationStatus.VERIFIED));
        verify(doctorRepository, never()).save(any(DoctorEntity.class));
        verify(notificationService, never()).createNotificationAsync(any(NotificationEntity.class));
    }

    @Test
    void testCheckLicenseExpiry_SuspendsExpiredDoctorsAndSendsNotifications() {
        Date expiryDate = new Date(System.currentTimeMillis() - 86400000); // Yesterday
        DoctorEntity doctor = DoctorEntity.builder()
                .doctorId("DOC-123")
                .firstName("John")
                .lastName("Doe")
                .verificationStatus(VerificationStatus.VERIFIED)
                .licenseExpiryDate(expiryDate)
                .build();

        List<DoctorEntity> expiredDoctors = new ArrayList<>();
        expiredDoctors.add(doctor);

        when(doctorRepository.findByLicenseExpiryDateBeforeAndVerificationStatus(any(Date.class), eq(VerificationStatus.VERIFIED)))
                .thenReturn(expiredDoctors);
        when(notificationService.createNotificationAsync(any(NotificationEntity.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.checkLicenseExpiry();

        // Verify doctor status was updated and saved
        verify(doctorRepository, times(1)).findByLicenseExpiryDateBeforeAndVerificationStatus(any(Date.class), eq(VerificationStatus.VERIFIED));
        
        ArgumentCaptor<DoctorEntity> doctorCaptor = ArgumentCaptor.forClass(DoctorEntity.class);
        verify(doctorRepository, times(1)).save(doctorCaptor.capture());
        
        DoctorEntity savedDoctor = doctorCaptor.getValue();
        assertEquals(VerificationStatus.SUSPENDED, savedDoctor.getVerificationStatus());
        assertNotNull(savedDoctor.getUpdatedAt());

        // Verify notification was constructed and dispatched correctly
        ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationService, times(1)).createNotificationAsync(notificationCaptor.capture());

        NotificationEntity notification = notificationCaptor.getValue();
        assertEquals("DOC-123", notification.getTargetId());
        assertEquals(NotificationRecipientType.INDIVIDUAL, notification.getRecipientType());
        assertEquals(NotificationType.SYSTEM, notification.getType());
        assertEquals("Practice Account Suspended", notification.getTitle());
        assertNotNull(notification.getMessage());
        assertNotNull(notification.getCreatedAt());
    }
}

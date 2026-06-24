package com.heal.doctor.services;

import com.heal.doctor.dto.CollaboratorDTO;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.DoctorAssociation;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.CollaboratorStatus;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.impl.CollaboratorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaboratorMultiDoctorTest {

    @Mock
    private CollaboratorProfileRepository collaboratorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CollaboratorServiceImpl collaboratorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeactivateCollaboratorWithMultipleAssociations() {
        // Collaborator works with Doc A and Doc B, currently active for both
        List<DoctorAssociation> associations = new ArrayList<>();
        associations.add(DoctorAssociation.builder()
                .doctorId("doc-a")
                .doctorName("Dr. A")
                .active(true)
                .build());
        associations.add(DoctorAssociation.builder()
                .doctorId("doc-b")
                .doctorName("Dr. B")
                .active(true)
                .build());

        CollaboratorProfileEntity profile = CollaboratorProfileEntity.builder()
                .collaboratorId("col-1")
                .doctorId("doc-a")
                .doctorAssociations(associations)
                .activeDoctorId("doc-a")
                .status(CollaboratorStatus.ACTIVATED)
                .build();

        UserEntity user = UserEntity.builder()
                .userId("col-1")
                .rolesEnum(RolesEnum.COLLABORATOR)
                .isActive(true)
                .build();

        when(collaboratorProfileRepository.findByCollaboratorId("col-1")).thenReturn(Optional.of(profile));
        when(userRepository.findByUserId("col-1")).thenReturn(Optional.of(user));

        // Doc A deactivates collaborator
        collaboratorService.deactivateCollaborator("col-1", "doc-a");

        // Captures
        ArgumentCaptor<CollaboratorProfileEntity> captor = ArgumentCaptor.forClass(CollaboratorProfileEntity.class);
        verify(collaboratorProfileRepository, times(1)).save(captor.capture());

        CollaboratorProfileEntity savedProfile = captor.getValue();
        
        // Association for Doc A should be inactive, Doc B should remain active
        assertFalse(savedProfile.getDoctorAssociations().get(0).isActive());
        assertTrue(savedProfile.getDoctorAssociations().get(1).isActive());

        // Since Doc B is still active, the global user account should NOT be deactivated
        verify(userRepository, never()).save(any(UserEntity.class));
        assertEquals(CollaboratorStatus.ACTIVATED, savedProfile.getStatus());

        // The active doctor should switch to the remaining active association (Doc B)
        assertEquals("doc-b", savedProfile.getActiveDoctorId());
    }

    @Test
    void testDeactivateCollaboratorWithOnlyOneAssociation() {
        // Collaborator works with Doc A only
        List<DoctorAssociation> associations = new ArrayList<>();
        associations.add(DoctorAssociation.builder()
                .doctorId("doc-a")
                .doctorName("Dr. A")
                .active(true)
                .build());

        CollaboratorProfileEntity profile = CollaboratorProfileEntity.builder()
                .collaboratorId("col-1")
                .doctorId("doc-a")
                .doctorAssociations(associations)
                .activeDoctorId("doc-a")
                .status(CollaboratorStatus.ACTIVATED)
                .build();

        UserEntity user = UserEntity.builder()
                .userId("col-1")
                .rolesEnum(RolesEnum.COLLABORATOR)
                .isActive(true)
                .build();

        when(collaboratorProfileRepository.findByCollaboratorId("col-1")).thenReturn(Optional.of(profile));
        when(userRepository.findByUserId("col-1")).thenReturn(Optional.of(user));

        // Doc A deactivates collaborator
        collaboratorService.deactivateCollaborator("col-1", "doc-a");

        // Captures
        ArgumentCaptor<CollaboratorProfileEntity> captor = ArgumentCaptor.forClass(CollaboratorProfileEntity.class);
        verify(collaboratorProfileRepository, times(1)).save(captor.capture());

        CollaboratorProfileEntity savedProfile = captor.getValue();
        assertFalse(savedProfile.getDoctorAssociations().get(0).isActive());

        // Since no other active associations exist, the global account MUST be deactivated
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().getIsActive());
        assertEquals(CollaboratorStatus.DEACTIVATED, savedProfile.getStatus());
        assertNull(savedProfile.getActiveDoctorId());
    }
}

package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AssociatedDoctorDTO;
import com.heal.doctor.dto.DoctorProfileDTO;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.DoctorAssociation;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.services.ICollaboratorDoctorService;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollaboratorDoctorServiceImpl implements ICollaboratorDoctorService {

    private static final Logger logger = LoggerFactory.getLogger(CollaboratorDoctorServiceImpl.class);

    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final DoctorRepository doctorRepository;

    @Override
    public List<AssociatedDoctorDTO> getAssociatedDoctors() {
        String userId = CurrentUserName.getCurrentUserId();
        logger.info("Fetching associated doctors for collaborator: {}", userId);

        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CollaboratorProfile", "collaboratorId"));

        List<DoctorAssociation> associations = profile.getDoctorAssociations();
        if (associations == null || associations.isEmpty()) {
            associations = new ArrayList<>();
            if (profile.getDoctorId() != null) {
                DoctorEntity doctor = doctorRepository.findByDoctorId(profile.getDoctorId()).orElse(null);
                associations.add(DoctorAssociation.builder()
                        .doctorId(profile.getDoctorId())
                        .doctorName(doctor != null ? "Dr. " + doctor.getFirstName() + " " + doctor.getLastName() : profile.getDoctorId())
                        .specialization(doctor != null ? doctor.getSpecialization() : null)
                        .clinicName(doctor != null ? doctor.getClinicName() : null)
                        .joinedAt(profile.getCreatedAt() != null ? profile.getCreatedAt() : new java.util.Date())
                        .active(profile.getStatus() == com.heal.doctor.models.enums.CollaboratorStatus.ACTIVATED)
                        .build());
                profile.setDoctorAssociations(associations);
                collaboratorProfileRepository.save(profile);
            }
        }

        if (associations.isEmpty()) {
            return List.of();
        }

        String activeDoctorId = profile.getEffectiveDoctorId();

        return associations.stream()
                .filter(DoctorAssociation::isActive)
                .map(assoc -> {
                    // Fetch latest profile picture from doctor entity
                    DoctorEntity doctor = doctorRepository.findByDoctorId(assoc.getDoctorId()).orElse(null);
                    return AssociatedDoctorDTO.builder()
                            .doctorId(assoc.getDoctorId())
                            .doctorName(assoc.getDoctorName())
                            .specialization(assoc.getSpecialization())
                            .clinicName(assoc.getClinicName())
                            .profilePicture(doctor != null ? doctor.getProfilePicture() : null)
                            .role(assoc.getRole())
                            .permissions(assoc.getPermissions())
                            .joinedAt(assoc.getJoinedAt())
                            .active(assoc.isActive())
                            .isCurrentActive(assoc.getDoctorId().equals(activeDoctorId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public AssociatedDoctorDTO switchActiveDoctor(String doctorId) {
        String userId = CurrentUserName.getCurrentUserId();
        logger.info("Switching active doctor for collaborator: {}, to doctorId: {}", userId, doctorId);

        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CollaboratorProfile", "collaboratorId"));

        List<DoctorAssociation> associations = profile.getDoctorAssociations();
        if (associations == null || associations.isEmpty()) {
            associations = new ArrayList<>();
            if (profile.getDoctorId() != null) {
                DoctorEntity doctor = doctorRepository.findByDoctorId(profile.getDoctorId()).orElse(null);
                associations.add(DoctorAssociation.builder()
                        .doctorId(profile.getDoctorId())
                        .doctorName(doctor != null ? "Dr. " + doctor.getFirstName() + " " + doctor.getLastName() : profile.getDoctorId())
                        .specialization(doctor != null ? doctor.getSpecialization() : null)
                        .clinicName(doctor != null ? doctor.getClinicName() : null)
                        .joinedAt(profile.getCreatedAt() != null ? profile.getCreatedAt() : new java.util.Date())
                        .active(profile.getStatus() == com.heal.doctor.models.enums.CollaboratorStatus.ACTIVATED)
                        .build());
                profile.setDoctorAssociations(associations);
                collaboratorProfileRepository.save(profile);
            }
        }

        if (associations.isEmpty()) {
            throw new BadRequestException("No doctor associations found for this collaborator");
        }

        // Validate the doctor is in the associations and active
        DoctorAssociation targetAssociation = associations.stream()
                .filter(a -> a.getDoctorId().equals(doctorId) && a.isActive())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("You are not actively associated with doctor: " + doctorId));

        // Update active doctor
        profile.setActiveDoctorId(doctorId);
        profile.setUpdatedAt(new java.util.Date());
        collaboratorProfileRepository.save(profile);

        logger.info("Active doctor switched successfully: collaboratorId: {}, activeDoctorId: {}", userId, doctorId);

        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId).orElse(null);

        return AssociatedDoctorDTO.builder()
                .doctorId(targetAssociation.getDoctorId())
                .doctorName(targetAssociation.getDoctorName())
                .specialization(targetAssociation.getSpecialization())
                .clinicName(targetAssociation.getClinicName())
                .profilePicture(doctor != null ? doctor.getProfilePicture() : null)
                .role(targetAssociation.getRole())
                .permissions(targetAssociation.getPermissions())
                .joinedAt(targetAssociation.getJoinedAt())
                .active(true)
                .isCurrentActive(true)
                .build();
    }

    @Override
    public DoctorProfileDTO getActiveDoctorProfile() {
        String userId = CurrentUserName.getCurrentUserId();
        logger.info("Fetching active doctor profile for collaborator: {}", userId);

        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CollaboratorProfile", "collaboratorId"));

        String activeDoctorId = profile.getEffectiveDoctorId();
        if (activeDoctorId == null) {
            throw new BadRequestException("No active doctor set for this collaborator");
        }

        DoctorEntity doctor = doctorRepository.findByDoctorId(activeDoctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "doctorId"));

        return DoctorProfileDTO.builder()
                .doctorId(doctor.getDoctorId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .specialization(doctor.getSpecialization())
                .phoneNumber(doctor.getPhoneNumber())
                .clinicName(doctor.getClinicName())
                .clinicAddress(doctor.getClinicAddress())
                .clinicEmail(doctor.getClinicEmail())
                .clinicContactNumber(doctor.getClinicContactNumber())
                .availability(doctor.getAvailability())
                .verificationStatus(doctor.getVerificationStatus() != null ? doctor.getVerificationStatus().name() : null)
                .profilePicture(doctor.getProfilePicture())
                .coverPicture(doctor.getCoverPicture())
                .about(doctor.getAbout())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .build();
    }
}

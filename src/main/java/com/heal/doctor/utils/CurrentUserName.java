package com.heal.doctor.utils;

import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.security.CollaboratorUserDetails;
import com.heal.doctor.security.DoctorUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserName {

    private static CollaboratorProfileRepository collaboratorProfileRepository;

    public CurrentUserName(CollaboratorProfileRepository collaboratorProfileRepository) {
        CurrentUserName.collaboratorProfileRepository = collaboratorProfileRepository;
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new RuntimeException("No authenticated user found or username is missing.");
    }

    public static String getCurrentDoctorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String doctorId) {
            if (RoleUtils.isCollaborator() && doctorId != null && collaboratorProfileRepository != null) {
                String userId = getCurrentUserId();
                Optional<CollaboratorProfileEntity> profileOpt = collaboratorProfileRepository.findByCollaboratorId(userId);
                if (profileOpt.isPresent()) {
                    CollaboratorProfileEntity profile = profileOpt.get();
                    boolean isValid = false;
                    if (profile.getDoctorAssociations() != null && !profile.getDoctorAssociations().isEmpty()) {
                        isValid = profile.getDoctorAssociations().stream()
                                .anyMatch(a -> a.getDoctorId().equals(doctorId) && a.isActive());
                    } else if (profile.getDoctorId() != null) {
                        isValid = profile.getDoctorId().equals(doctorId) && 
                                (profile.getStatus() == com.heal.doctor.models.enums.CollaboratorStatus.ACTIVATED);
                    }
                    if (!isValid) {
                        throw new com.heal.doctor.exception.ForbiddenException("Collaborator is not actively associated with doctor: " + doctorId);
                    }
                }
            }
            return doctorId;
        }
        // Fallback: try to get from UserDetails
        if (authentication != null && authentication.getPrincipal() instanceof DoctorUserDetails doctorUserDetails) {
            return doctorUserDetails.getDoctorId();
        }
        if (authentication != null && authentication.getPrincipal() instanceof CollaboratorUserDetails collaboratorUserDetails) {
            return collaboratorUserDetails.getDoctorId();
        }
        return null;
    }

    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof DoctorUserDetails doctorUserDetails) {
            return doctorUserDetails.getDoctorId();
        }
        if (authentication != null && authentication.getPrincipal() instanceof CollaboratorUserDetails collaboratorUserDetails) {
            return collaboratorUserDetails.getUserId();
        }
        throw new RuntimeException("No authenticated user found or user ID is missing.");
    }

    public static String getCurrentUserRole() {
        return RoleUtils.getCurrentUserRole();
    }
}


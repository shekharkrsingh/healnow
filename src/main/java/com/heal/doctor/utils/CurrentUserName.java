package com.heal.doctor.utils;

import com.heal.doctor.security.CollaboratorUserDetails;
import com.heal.doctor.security.DoctorUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserName {

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new RuntimeException("No authenticated user found or username is missing.");
    }

    public static String getCurrentDoctorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // For doctors: doctorId = userId (their own ID)
        // For collaborators: doctorId = associated doctor's ID
        // Both are stored in credentials by JwtAuthenticationFilter
        if (authentication != null && authentication.getCredentials() instanceof String doctorId) {
            return doctorId;
        }
        // Fallback: try to get from UserDetails
        if (authentication != null && authentication.getPrincipal() instanceof DoctorUserDetails doctorUserDetails) {
            return doctorUserDetails.getDoctorId();
        }
        if (authentication != null && authentication.getPrincipal() instanceof CollaboratorUserDetails collaboratorUserDetails) {
            return collaboratorUserDetails.getDoctorId();
        }
        throw new RuntimeException("No authenticated user found or doctor ID is missing.");
    }

    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Get the actual logged-in user's ID (regardless of role)
        if (authentication != null && authentication.getPrincipal() instanceof DoctorUserDetails doctorUserDetails) {
            return doctorUserDetails.getDoctorId(); // For doctors, userId = doctorId
        }
        if (authentication != null && authentication.getPrincipal() instanceof CollaboratorUserDetails collaboratorUserDetails) {
            return collaboratorUserDetails.getUserId(); // For collaborators, get their own ID
        }
        throw new RuntimeException("No authenticated user found or user ID is missing.");
    }

    public static String getCurrentUserRole() {
        return RoleUtils.getCurrentUserRole();
    }
}

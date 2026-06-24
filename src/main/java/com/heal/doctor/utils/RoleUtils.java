package com.heal.doctor.utils;

import com.heal.doctor.repositories.CollaboratorProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Utility class for role checking and role-related operations.
 */
@Component
public class RoleUtils {

    private static CollaboratorProfileRepository collaboratorProfileRepository;

    public RoleUtils(CollaboratorProfileRepository collaboratorProfileRepository) {
        RoleUtils.collaboratorProfileRepository = collaboratorProfileRepository;
    }

    /**
     * Get the current user's role from SecurityContext.
     * 
     * @return The role name (e.g., "DOCTOR", "ADMIN", "USER") or null if not found
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                String authorityName = authority.getAuthority();
                // Spring Security uses ROLE_ prefix, so we need to remove it
                if (authorityName.startsWith("ROLE_")) {
                    return authorityName.substring(5); // Remove "ROLE_" prefix
                }
                return authorityName;
            }
        }
        return null;
    }

    /**
     * Check if the current user has a specific role.
     * 
     * @param role The role to check (e.g., "DOCTOR", "ADMIN", "USER")
     * @return true if the user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            String roleWithPrefix = "ROLE_" + role.toUpperCase();
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
        }
        return false;
    }

    /**
     * Check if the current user is an admin.
     * 
     * @return true if the user has ADMIN role, false otherwise
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if the current user is a doctor.
     * 
     * @return true if the user has DOCTOR role, false otherwise
     */
    public static boolean isDoctor() {
        return hasRole("DOCTOR");
    }

    /**
     * Check if the current user is a collaborator.
     * 
     * @return true if the user has COLLABORATOR role, false otherwise
     */
    public static boolean isCollaborator() {
        return hasRole("COLLABORATOR");
    }

    /**
     * Check if the current user is an admin or owns the resource.
     * 
     * @param ownerId The ID of the resource owner
     * @param requesterId The ID of the requester (current user)
     * @return true if the user is admin or owns the resource, false otherwise
     */
    public static boolean isAdminOrOwner(String ownerId, String requesterId) {
        if (isAdmin()) {
            return true;
        }
        return ownerId != null && ownerId.equals(requesterId);
    }

    /**
     * Get the associated doctor ID for the current user.
     * For doctors, returns their own ID. For collaborators, returns the doctor they work for.
     * 
     * @return The doctor ID associated with the current user
     */
    public static String getAssociatedDoctorId() {
        return CurrentUserName.getCurrentDoctorId();
    }

    /**
     * Check if the current user is an admin, owns the resource, or is a collaborator for the resource owner.
     * 
     * @param ownerId The ID of the resource owner (e.g., appointment's doctorId)
     * @param requesterId The ID of the requester (current user's userId - for doctors this equals doctorId, for collaborators this is their own userId)
     * @return true if the user is admin, owns the resource, or is a collaborator for the owner, false otherwise
     */
    public static boolean isAdminOrOwnerOrCollaborator(String ownerId, String requesterId) {
        if (isAdmin()) {
            return true;
        }
        // Check if requester is the owner (for doctors, requesterId = doctorId = ownerId)
        if (ownerId != null && ownerId.equals(requesterId)) {
            return true;
        }
        // Check if requester is a collaborator working for the owner
        if (isCollaborator()) {
            // For collaborators, get the doctorId they work for (not their own userId)
            String associatedDoctorId = getAssociatedDoctorId();
            return ownerId != null && ownerId.equals(associatedDoctorId);
        }
        return false;
    }

    /**
     * Check if the current user is a collaborator associated with a specific doctor.
     *
     * @param doctorId The doctor ID to check association for
     * @return true if the current user is a collaborator actively associated with the given doctor
     */
    public static boolean isCollaboratorForDoctor(String doctorId) {
        if (!isCollaborator()) return false;
        String userId = CurrentUserName.getCurrentUserId();
        if (collaboratorProfileRepository == null) return false;
        return collaboratorProfileRepository.findByCollaboratorId(userId)
                .map(profile -> {
                    if (profile.getDoctorAssociations() != null && !profile.getDoctorAssociations().isEmpty()) {
                        return profile.getDoctorAssociations().stream()
                                .anyMatch(a -> a.getDoctorId().equals(doctorId) && a.isActive());
                    }
                    return profile.getDoctorId() != null && 
                           profile.getDoctorId().equals(doctorId) && 
                           profile.getStatus() == com.heal.doctor.models.enums.CollaboratorStatus.ACTIVATED;
                })
                .orElse(false);
    }
}


package com.heal.doctor.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * Utility class for role checking and role-related operations.
 */
public class RoleUtils {

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
}

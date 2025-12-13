package com.heal.doctor.models.enums;

/**
 * Defines who should receive a notification
 */
public enum NotificationTargetType {
    /**
     * Notification visible only to a specific user (collaborator)
     * Requires userId to be set, doctorId should be the associated doctor's ID
     */
    USER_SPECIFIC,
    
    /**
     * Notification visible only to the doctor
     * Requires doctorId to be set, userId must be null
     */
    DOCTOR_ONLY,
    
    /**
     * Notification visible to both doctor and all collaborators
     * Requires doctorId to be set, userId must be null
     */
    DOCTOR_AND_COLLABORATORS,
    
    /**
     * Notification visible to all collaborators of a doctor (but not the doctor)
     * Requires doctorId to be set, userId must be null
     */
    ALL_COLLABORATORS,
    
    /**
     * Notification visible only to admin users
     * doctorId and userId must be null
     */
    ADMIN_ONLY,
    
    /**
     * Broadcast notification visible to everyone (doctors, collaborators, admins)
     * doctorId and userId must be null
     */
    BROADCAST
}

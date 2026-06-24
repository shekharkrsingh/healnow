package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Embedded document representing a collaborator's association with a doctor.
 * Supports many-to-many: one collaborator can work with multiple doctors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAssociation {

    private String doctorId;
    private String doctorName;       // Cached: "Dr. FirstName LastName"
    private String specialization;   // Cached for quick display
    private String clinicName;       // Cached for quick display
    private String role;             // e.g., RECEPTIONIST, NURSE, ASSISTANT
    private List<String> permissions;
    private Date joinedAt;

    @Builder.Default
    private boolean active = true;   // false = removed/deactivated for this doctor
}

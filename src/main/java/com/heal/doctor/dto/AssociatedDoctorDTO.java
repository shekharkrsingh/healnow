package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssociatedDoctorDTO {
    private String doctorId;
    private String doctorName;
    private String specialization;
    private String clinicName;
    private String profilePicture;
    private String role;
    private List<String> permissions;
    private Date joinedAt;
    private boolean active;
    private boolean isCurrentActive;
}

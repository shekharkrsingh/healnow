package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorSettingsDTO {
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;
    
    private boolean publicBookingAllowed;
    private boolean enableEmergencyFeature;
    
    private Date updatedAt;
}

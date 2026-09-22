package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultDoctorSettingsDTO {
    private boolean publicBookingAllowed;
    private boolean enableEmergencyFeature;
    private Date updatedAt;
}

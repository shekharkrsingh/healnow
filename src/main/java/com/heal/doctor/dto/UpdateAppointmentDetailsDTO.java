package com.heal.doctor.dto;

import com.heal.doctor.models.enums.AppointmentStatus;
import lombok.Data;

@Data
public class UpdateAppointmentDetailsDTO {
    private AppointmentStatus appointmentStatus;
    private Boolean paymentStatus;
    private Boolean availableAtClinic;
    private Boolean treated;
}

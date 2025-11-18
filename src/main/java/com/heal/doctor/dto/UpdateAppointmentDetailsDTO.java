package com.heal.doctor.dto;

import com.heal.doctor.models.enums.AppointmentStatus;
import lombok.Data;

import java.util.Date;

@Data
public class UpdateAppointmentDetailsDTO {
    private AppointmentStatus appointmentStatus;
    private Boolean paymentStatus;
    private Boolean availableAtClinic;
    private Boolean treated;
    private String patientName;
    private String contact;
    private String email;
    private String description;
    private Date appointmentDateTime;
}

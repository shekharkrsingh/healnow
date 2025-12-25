package com.heal.doctor.dto;

import com.heal.doctor.models.enums.AppointmentStatus;
import lombok.Data;

import java.util.Date;

@Data
public class UpdateAppointmentDetailsDTO {
    private String patientName;
    private AppointmentStatus appointmentStatus;
    private Boolean paymentStatus;
    private Boolean availableAtClinic;
    private Boolean treated;
    private String contact;
    private String description;
    private String email;
    private Date appointmentDateTime;
}

package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {
    private String patientName;
    private String contact;
    private String email;
    private String description;
    private Date appointmentDateTime;
    private Boolean availableAtClinic;
    private Boolean paymentStatus;
}

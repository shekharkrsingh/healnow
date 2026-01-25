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
public class PatientSelfBookingDTO {
    private String patientName;
    private String contact;
    private Date appointmentDateTime;
    private String doctorId;
    private String description;
}

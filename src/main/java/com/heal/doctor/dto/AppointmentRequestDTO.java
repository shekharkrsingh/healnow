package com.heal.doctor.dto;

import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
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
    private String description;
    private Date bookingDateTime;
    private Boolean availableAtClinic;
    private Boolean paymentStatus;
}

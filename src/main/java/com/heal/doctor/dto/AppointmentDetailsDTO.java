package com.heal.doctor.dto;

import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import lombok.Data;

import java.util.Date;

@Data
public class AppointmentDetailsDTO {
    private String appointmentId;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String patientName;
    private String contact;
    private String email;
    private String description;
    private String notice;
    private Date appointmentDateTime;
    private Date bookingDateTime;
    private Boolean availableAtClinic;
    private Date availableAtClinicDateTime;
    private Boolean treated;
    private Date treatedDateTime;
    private AppointmentStatus status;
    private AppointmentType appointmentType;
    private Boolean paymentStatus;
    private Boolean isEmergency;
}

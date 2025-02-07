package com.heal.doctor.dto;

import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;

import java.util.Date;

public class AppointmentDTO {
    private String appointmentId;
    private String doctorId;
    private String patientName;
    private String contact;
    private String description;
    private Date appointmentDateTime;
    private Date bookingDateTime;
    private Boolean availableAtClinic;
    private Boolean treated;
    private Date treatedDateTime;
    private AppointmentStatus status;
    private AppointmentType appointmentType;
    private Boolean paymentStatus;
}

package com.heal.doctor.dto;

import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Builder
@Data
public class AppointmentSearchDTO {
    private String appointmentId;
    private String patientName;
    private String contact;
    private String email;
    private String appointmentDate;
    private String bookingDate;
    private String status;
    private Boolean treated;
    private AppointmentType appointmentType;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}

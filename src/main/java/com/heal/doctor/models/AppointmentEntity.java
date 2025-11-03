package com.heal.doctor.models;

import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "appointments")
public class AppointmentEntity {

    @Id
    private String id;
    private String appointmentId;
    private String doctorId;
    private String patientName;
    private String contact;
    private String email;
    private String description;
    private Date appointmentDateTime;
    private Date bookingDateTime;
    private Boolean availableAtClinic;
    private Boolean treated;
    private Date treatedDateTime;
    private AppointmentStatus status;
    private AppointmentType appointmentType;
    private Boolean paymentStatus;
    private Boolean isEmergency;
}


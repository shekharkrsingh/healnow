package com.heal.doctor.models;

import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "appointments")
@CompoundIndexes({
    @CompoundIndex(name = "doctor_appt_date_idx", def = "{'doctorId': 1, 'appointmentDateTime': 1}"),
    @CompoundIndex(name = "doctor_booking_date_idx", def = "{'doctorId': 1, 'bookingDateTime': 1}"),
    @CompoundIndex(name = "doctor_treated_date_idx", def = "{'doctorId': 1, 'treatedDateTime': 1}"),
    @CompoundIndex(name = "doctor_status_treated_idx", def = "{'doctorId': 1, 'status': 1, 'treated': 1}"),
    @CompoundIndex(name = "doctor_clinic_treated_idx", def = "{'doctorId': 1, 'availableAtClinic': 1, 'treated': 1}"),
    @CompoundIndex(name = "doctor_patient_contact_date_status_idx", def = "{'doctorId': 1, 'patientName': 1, 'contact': 1, 'appointmentDateTime': 1, 'status': 1}")
})
public class AppointmentEntity {

    @Id
    private String id;

    @Indexed(unique = true, name = "appointment_id_idx")
    private String appointmentId;

    @Indexed(name = "doctor_id_idx")
    private String doctorId;

    private String patientName;
    private String contact;
    private String email;
    private String description;

    @Indexed(name = "appointment_date_idx")
    private Date appointmentDateTime;

    @Indexed(name = "booking_date_idx")
    private Date bookingDateTime;

    private Boolean availableAtClinic;
    private Boolean treated;

    @Indexed(name = "treated_date_idx")
    private Date treatedDateTime;

    private AppointmentStatus status;
    private AppointmentType appointmentType;
    private Boolean paymentStatus;
    private Boolean isEmergency;
}


package com.heal.doctor.models;

import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Appointment ID is required")
    @Size(max = 50, message = "Appointment ID must not exceed 50 characters")
    private String appointmentId;

    @Indexed(name = "doctor_id_idx")
    @NotBlank(message = "Doctor ID is required")
    @Size(max = 50, message = "Doctor ID must not exceed 50 characters")
    private String doctorId;

    @NotBlank(message = "Patient name is required")
    @Size(min = 2, max = 100, message = "Patient name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Patient name can only contain letters, spaces, hyphens, and apostrophes")
    private String patientName;

    @NotBlank(message = "Contact number is required")
    @Size(min = 10, max = 15, message = "Contact number must be between 10 and 15 digits")
    @Pattern(regexp = "^\\d+$", message = "Contact number must contain only digits")
    private String contact;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Indexed(name = "appointment_date_idx")
    private Date appointmentDateTime;

    @Indexed(name = "booking_date_idx")
    private Date bookingDateTime;

    @NotNull(message = "Available at clinic status is required")
    private Boolean availableAtClinic;

    @NotNull(message = "Treated status is required")
    private Boolean treated;

    @Indexed(name = "treated_date_idx")
    private Date treatedDateTime;

    @NotNull(message = "Appointment status is required")
    private AppointmentStatus status;

    private AppointmentType appointmentType;

    @NotNull(message = "Payment status is required")
    private Boolean paymentStatus;

    @NotNull(message = "Emergency status is required")
    private Boolean isEmergency;
}


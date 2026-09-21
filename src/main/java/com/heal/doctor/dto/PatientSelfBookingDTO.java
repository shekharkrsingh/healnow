package com.heal.doctor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Patient name is required")
    @Size(min = 2, max = 100, message = "Patient name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Patient name can only contain letters, spaces, hyphens, and apostrophes")
    private String patientName;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Contact number must be exactly 10 digits")
    private String contact;

    private Date appointmentDateTime;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

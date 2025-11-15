package com.heal.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {
    
    private static final int CONTACT_LENGTH = 10;
    private static final String CONTACT_PATTERN = "^\\d{10}$";
    
    @NotBlank(message = "Patient name is required")
    private String patientName;
    
    @NotBlank(message = "Contact number is required")
    @Size(min = CONTACT_LENGTH, max = CONTACT_LENGTH, message = "Contact number must be exactly " + CONTACT_LENGTH + " digits")
    @Pattern(regexp = CONTACT_PATTERN, message = "Contact number must be exactly " + CONTACT_LENGTH + " digits")
    private String contact;
    
    private String email;
    
    private String description;
    
    private Date appointmentDateTime;
    
    @NotNull(message = "Available at clinic status is required")
    private Boolean availableAtClinic;
    
    @NotNull(message = "Payment status is required")
    private Boolean paymentStatus;
}

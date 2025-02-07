package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorRegistrationDTO {
    private String firstName;
    private String lastName;

    private String email;
    private String password;

    private String otp;
}

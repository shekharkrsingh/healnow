package com.heal.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "Username is required")
    @Size(max = 255, message = "Username must not exceed 255 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must not exceed 72 characters")
    private String password;
}

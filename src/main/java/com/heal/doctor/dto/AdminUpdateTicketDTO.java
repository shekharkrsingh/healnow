package com.heal.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateTicketDTO {
    
    @NotBlank(message = "Status is required")
    private String status;

    @Size(max = 2000, message = "Admin response must not exceed 2000 characters")
    private String adminResponse;
}

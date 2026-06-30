package com.heal.doctor.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendTerminateRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}

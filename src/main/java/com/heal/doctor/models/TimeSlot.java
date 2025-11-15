package com.heal.doctor.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {
    @NotBlank(message = "Start time is required")
    @Size(max = 10, message = "Start time must not exceed 10 characters")
    @Pattern(regexp = "^(0?[1-9]|1[0-2]):[0-5][0-9]\\s?(AM|PM|am|pm)$", message = "Start time must be in format HH:MM AM/PM")
    private String startTime;

    @NotBlank(message = "End time is required")
    @Size(max = 10, message = "End time must not exceed 10 characters")
    @Pattern(regexp = "^(0?[1-9]|1[0-2]):[0-5][0-9]\\s?(AM|PM|am|pm)$", message = "End time must be in format HH:MM AM/PM")
    private String endTime;
}

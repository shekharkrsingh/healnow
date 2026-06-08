package com.heal.doctor.models;

import com.heal.doctor.models.enums.AvailableDayEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayAvailability {

    @NotNull(message = "Day is required")
    private AvailableDayEnum day;

    @Size(max = 50, message = "Available time slots cannot exceed 50 per day")
    @Valid
    private List<TimeSlot> slots;
}

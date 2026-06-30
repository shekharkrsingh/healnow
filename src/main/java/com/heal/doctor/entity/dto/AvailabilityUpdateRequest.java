package com.heal.doctor.entity.dto;

import com.heal.doctor.models.DayAvailability;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AvailabilityUpdateRequest {

    @NotNull
    @Valid
    private List<DayAvailability> availability;
}

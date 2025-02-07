package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {
    private String startTime; // e.g., "10:00 AM"
    private String endTime;   // e.g., "11:00 AM"
}

package com.heal.doctor.dto;

import com.heal.doctor.models.DailyTreatedPatients;
import lombok.Data;

import java.util.List;

@Data
public class DoctorStatisticsDTO {
    Integer totalAppointment;
    Integer totalUntreatedAppointment;
    Integer totalTreatedAppointment;
    Integer totalAvailableAtClinic;
    List<DailyTreatedPatients> lastWeekTreatedData;
}

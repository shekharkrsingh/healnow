package com.heal.doctor.dto;

import com.heal.doctor.models.DailyTreatedPatients;
import lombok.Data;

import java.util.List;

@Data
public class DoctorStatisticsDTO {
    private Integer totalAppointment;
    private Integer totalUntreatedAppointment;
    private Integer totalTreatedAppointment;
    private Integer totalAvailableAtClinic;
    private List<DailyTreatedPatients> lastWeekTreatedData;
    private Integer lastActiveDayAppointments;
    private Integer lastActiveDayTreatedAppointments;

}

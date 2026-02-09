package com.heal.doctor.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteStatisticsDTO {
    private Integer totalPatients;
    private Integer totalDoctors;
    private Integer satisfactionRate;
    private String avgWaitTime;
    private String monthlyGrowth;
    private Integer specialtiesCount;
    private Integer citiesCount;
}

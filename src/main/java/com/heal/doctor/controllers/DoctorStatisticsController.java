package com.heal.doctor.controllers;

import com.heal.doctor.dto.DoctorStatisticsDTO;
import com.heal.doctor.services.IDoctorStatisticsService;
import com.heal.doctor.services.impl.DoctorStatisticsServiceImpl;
import com.heal.doctor.utils.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/doctor/statistics")
public class DoctorStatisticsController {

    private final IDoctorStatisticsService appointmentStatisticsService;



    @GetMapping
    public ResponseEntity<ApiResponse<DoctorStatisticsDTO>> getDoctorStatistics(){
        DoctorStatisticsDTO doctorStatisticsDTO= appointmentStatisticsService.fetchStatistics();
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor Statistics fetched Successfully", doctorStatisticsDTO));
    }
}

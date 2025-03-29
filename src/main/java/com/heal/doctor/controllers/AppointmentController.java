package com.heal.doctor.controllers;

import com.heal.doctor.dto.*;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.services.IAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final IAppointmentService appointmentService;

    public AppointmentController(IAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<AppointmentDTO>> bookAppointment(@RequestBody AppointmentRequestDTO requestDTO) {
        AppointmentDTO appointment = appointmentService.bookAppointment(requestDTO);
        return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                .success(true)
                .message("Appointment booked successfully")
                .data(appointment)
                .build());
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentDTO>> getAppointmentById(@PathVariable String appointmentId) {
        AppointmentDTO appointment = appointmentService.getAppointmentById(appointmentId);
        return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                .success(true)
                .message("Appointment fetched successfully")
                .data(appointment)
                .build());
    }

    @GetMapping("/by-doctor")
    public ResponseEntity<ApiResponse<List<AppointmentDTO>>> getAppointmentsByDoctorAndDate(@RequestParam(value = "date", required = false) String date){
        List<AppointmentDTO> appointments;
        if(date==null || date.trim().isEmpty()){
            appointments = appointmentService
                    .getAppointmentsByBookingDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        else{
            appointments = appointmentService.getAppointmentsByBookingDate(date);
        }
        return ResponseEntity.ok(ApiResponse.<List<AppointmentDTO>>builder()
                .success(true)
                .message("Appointments fetched successfully")
                .data(appointments)
                .build());
    }

    @PutMapping("/update/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentDTO>> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody UpdateAppointmentDetailsDTO updateDTO) {

        AppointmentDTO appointment = null;

        if (updateDTO.getAppointmentStatus() != null) {
            appointment = appointmentService.updateAppointmentStatus(appointmentId, updateDTO.getAppointmentStatus());
        }
        if (updateDTO.getPaymentStatus() != null) {
            appointment = appointmentService.updatePaymentStatus(appointmentId, updateDTO.getPaymentStatus());
        }
        if (updateDTO.getTreated() != null) {
            appointment = appointmentService.updateTreatedStatus(appointmentId, updateDTO.getTreated());
        }
        if (updateDTO.getAvailableAtClinic() != null) {
            appointment = appointmentService.updateAvailableAtClinic(appointmentId, updateDTO.getAvailableAtClinic());
        }

        return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                .success(true)
                .message("Appointment updated successfully")
                .data(appointment)
                .build());
    }
}
package com.heal.doctor.controllers;


import com.heal.doctor.services.IAppointmentService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.dto.*;
import com.heal.doctor.services.IDoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/doctors")
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class DoctorController {


    private final IDoctorService doctorService;
    private final IAppointmentService appointmentService;


    @PutMapping()
    public ResponseEntity<ApiResponse<DoctorProfileDTO>> updateDoctor(@Valid @RequestBody UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
        DoctorProfileDTO savedDoctorDTO = doctorService.updateDoctor(updateDoctorDetailsDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor updated successfully", savedDoctorDTO));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileDTO>> getDoctorProfile() {
        DoctorProfileDTO doctorDTO = doctorService.getDoctorProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", doctorDTO));
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentDetailsDTO>> getAppointmentDetails(@PathVariable String appointmentId) {
        AppointmentDetailsDTO detailsDTO = appointmentService.getAppointmentDetails(appointmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment details retrieved successfully", detailsDTO));
    }

}

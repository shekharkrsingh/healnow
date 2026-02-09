package com.heal.doctor.controllers;

import com.heal.doctor.dto.RogerProfileDTO;
import com.heal.doctor.dto.RogerUpdateDTO;
import com.heal.doctor.dto.AppointmentDetailsDTO;
import com.heal.doctor.services.IRogerService;
import com.heal.doctor.services.IAppointmentService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rogers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROGER')")
public class RogerController {

    private final IRogerService rogerService;
    private final IAppointmentService appointmentService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<RogerProfileDTO>> getRogerProfile() {
        RogerProfileDTO profile = rogerService.getRogerProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<RogerProfileDTO>> updateRogerProfile(@RequestBody RogerUpdateDTO updateDTO) {
        RogerProfileDTO profile = rogerService.updateRogerProfile(updateDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully", profile));
    }

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentDetailsDTO>>> getRogerAppointments() {
        String email = CurrentUserName.getCurrentUsername();
        List<AppointmentDetailsDTO> appointments = appointmentService.getAppointmentsByPatientEmail(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointments retrieved successfully", appointments));
    }
}

package com.heal.doctor.controllers;


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


    @PutMapping()
    public ResponseEntity<ApiResponse<UserDTO>> updateDoctor(@Valid @RequestBody UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
        UserDTO savedDoctorDTO = doctorService.updateDoctor(updateDoctorDetailsDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor updated successfully", savedDoctorDTO));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getDoctorProfile() {
        UserDTO doctorDTO = doctorService.getDoctorProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", doctorDTO));
    }

}

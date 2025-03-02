package com.heal.doctor.controllers;


import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.dto.*;
import com.heal.doctor.services.IDoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/doctors")

public class DoctorController {


    private final IDoctorService doctorService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getAllDoctors() {
        List<DoctorDTO> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", doctors));
    }

    @PutMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorDTO>> updateDoctor(@PathVariable  @RequestBody DoctorDTO doctorDTO) {
        DoctorDTO savedDoctorDTO = doctorService.updateDoctor(doctorDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor updated successfully", savedDoctorDTO));
    }


//Delete service is not available for use


//    @DeleteMapping("/{doctorId}")
//    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable String doctorId) {
//        doctorService.deleteDoctor(doctorId);
//        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor deleted successfully", null));
//    }


    @PostMapping("/update-email")
    public ResponseEntity<ApiResponse<String>> updateEmail(@RequestBody UpdateEmailDTO updateEmailDTO, @RequestParam String doctorId) {
        doctorService.updateEmail(updateEmailDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Email updated successfully", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        doctorService.changePassword(changePasswordDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorDTO>> getDoctorProfile() {
        DoctorDTO doctorDTO = doctorService.getDoctorProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", doctorDTO));
    }



}

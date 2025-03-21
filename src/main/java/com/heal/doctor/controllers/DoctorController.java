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




    @PutMapping()
    public ResponseEntity<ApiResponse<DoctorDTO>> updateDoctor(@RequestBody UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
        DoctorDTO savedDoctorDTO = doctorService.updateDoctor(updateDoctorDetailsDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor updated successfully", savedDoctorDTO));
    }



    @PostMapping("/update-email")
    public ResponseEntity<ApiResponse<String>> updateEmail(@RequestBody UpdateEmailDTO updateEmailDTO) {
        System.out.println(updateEmailDTO.toString());
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

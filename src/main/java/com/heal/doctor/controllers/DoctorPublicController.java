package com.heal.doctor.controllers;

import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.IDoctorStatisticsService;
import com.heal.doctor.Mail.IOtpService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class DoctorPublicController {

    private final IDoctorService doctorService;
    private final IOtpService otpService;
    private final IDoctorStatisticsService appointmentStatisticsService;

    @GetMapping
    public String test(){
        return "Server is Up!";
    }


    @GetMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorDTO>> getDoctorById(@PathVariable String doctorId) {
        DoctorDTO doctorDTO = doctorService.getDoctorById(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor retrieved successfully", doctorDTO));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorDTO>> createDoctor(@RequestBody DoctorRegistrationDTO doctorRegistrationDTO) {
        DoctorDTO doctorDTO = doctorService.createDoctor(doctorRegistrationDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor created successfully", doctorDTO));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        doctorService.forgotPassword(forgotPasswordDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successfully", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        String token = doctorService.loginDoctor(loginRequestDTO.getUsername(), loginRequestDTO.getPassword());
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "login successfully", loginResponseDTO));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OtpResponseDTO>> sendOtp(@RequestBody OtpRequestDTO otpRequestDTO){
        OtpResponseDTO otpResponseDTO= otpService.generateOtp(otpRequestDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP is generated successfully", otpResponseDTO));
    }
}

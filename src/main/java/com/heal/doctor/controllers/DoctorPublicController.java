package com.heal.doctor.controllers;

import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.IDoctorStatisticsService;
import com.heal.doctor.Mail.IOtpService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class DoctorPublicController {

    private final IDoctorService doctorService;
    private final IOtpService otpService;
    private final IDoctorStatisticsService appointmentStatisticsService;

    @Value("${app.version.minimum}")
    private String minimumVersion;

    @Value("${app.version.latest}")
    private String latestVersion;

    @Value("${app.version.forceUpdate}")
    private boolean forceUpdate;

    @Value("${app.version.websiteUrl}")
    private String websiteUrl;

    @Value("${app.version.message}")
    private String updateMessage;

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

    @GetMapping("/app/version-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAppVersion() {
        if (minimumVersion == null || minimumVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("Minimum version is not configured");
        }
        
        if (websiteUrl == null || websiteUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Website URL is not configured");
        }
        
        Map<String, Object> versionData = new HashMap<>();
        versionData.put("minimumVersion", minimumVersion.trim());
        versionData.put("latestVersion", (latestVersion != null && !latestVersion.trim().isEmpty()) 
            ? latestVersion.trim() 
            : minimumVersion.trim());
        versionData.put("forceUpdate", forceUpdate);
        versionData.put("websiteUrl", websiteUrl.trim());
        versionData.put("message", (updateMessage != null && !updateMessage.trim().isEmpty()) 
            ? updateMessage.trim() 
            : String.format("A new version is available. Please update to version %s or higher to continue using the app.", minimumVersion));
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Version check successful", versionData));
    }
}

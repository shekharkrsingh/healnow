package com.heal.doctor.controllers;

import com.heal.doctor.services.IAppointmentService;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.Mail.IOtpService;
import com.heal.doctor.services.IUserService;
import com.heal.doctor.services.impl.RuntimeApplicationConfigServices;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class UserPublicController {

    private final IDoctorService doctorService;
    private final IUserService userService;
    private final IOtpService otpService;
    private final IAppointmentService appointmentService;
    private final RuntimeApplicationConfigServices runtimeApplicationConfigService;


    @GetMapping
    public String test(){
        return "Server is Up!";
    }


    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<DoctorPublicProfileDTO>>> getAllDoctors(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String query) {
        
        List<DoctorProfileDTO> doctorDTOs = doctorService.getAllDoctors(location, query);
        List<DoctorPublicProfileDTO> publicProfiles = doctorDTOs.stream()
                .map(doctorDTO -> DoctorPublicProfileDTO.builder()
                        .doctorId(doctorDTO.getDoctorId())
                        .firstName(doctorDTO.getFirstName())
                        .lastName(doctorDTO.getLastName())
                        .specialization(doctorDTO.getSpecialization())
                        .clinicName(doctorDTO.getClinicName())
                        .clinicEmail(doctorDTO.getClinicEmail())
                        .clinicContactNumber(doctorDTO.getClinicContactNumber())
                        .clinicAddress(doctorDTO.getClinicAddress())
                        .address(doctorDTO.getAddress())
                        .about(doctorDTO.getAbout())
                        .bio(doctorDTO.getBio())
                        .yearsOfExperience(doctorDTO.getYearsOfExperience())
                        .profilePicture(doctorDTO.getProfilePicture())
                        .gender(doctorDTO.getGender())
                        .availability(doctorDTO.getAvailability())
                        .education(doctorDTO.getEducation())
                        .achievementsAndAwards(doctorDTO.getAchievementsAndAwards())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", publicProfiles));
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorPublicProfileDTO>> getDoctorById(@PathVariable String doctorId) {
        DoctorProfileDTO doctorDTO = doctorService.getDoctorById(doctorId);
        
        DoctorPublicProfileDTO publicProfile = DoctorPublicProfileDTO.builder()
                .doctorId(doctorDTO.getDoctorId())
                .firstName(doctorDTO.getFirstName())
                .lastName(doctorDTO.getLastName())
                .specialization(doctorDTO.getSpecialization())
                .clinicName(doctorDTO.getClinicName())
                .clinicEmail(doctorDTO.getClinicEmail())
                .clinicContactNumber(doctorDTO.getClinicContactNumber())
                .clinicAddress(doctorDTO.getClinicAddress())
                .address(doctorDTO.getAddress())
                .about(doctorDTO.getAbout())
                .bio(doctorDTO.getBio())
                .yearsOfExperience(doctorDTO.getYearsOfExperience())
                .profilePicture(doctorDTO.getProfilePicture())
                .gender(doctorDTO.getGender())
                .availability(doctorDTO.getAvailability())
                .education(doctorDTO.getEducation())
                .achievementsAndAwards(doctorDTO.getAchievementsAndAwards())
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor retrieved successfully", publicProfile));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorProfileDTO>> createDoctor(@RequestBody DoctorRegistrationDTO doctorRegistrationDTO) {
        DoctorProfileDTO doctorDTO = doctorService.createDoctor(doctorRegistrationDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor created successfully", doctorDTO));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        userService.forgotPassword(forgotPasswordDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successfully", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        LoginResponseDTO loginResponseDTO = userService.login(loginRequestDTO.getUsername(), loginRequestDTO.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, "login successfully", loginResponseDTO));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> refreshTokens(@Valid @RequestBody RefreshTokenRequestDTO requestDTO) {
        LoginResponseDTO loginResponseDTO = userService.refreshAccessToken(requestDTO.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Tokens refreshed successfully", loginResponseDTO));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody RefreshTokenRequestDTO requestDTO) {
        userService.revokeRefreshToken(requestDTO.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out successfully", null));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OtpResponseDTO>> sendOtp(@RequestBody OtpRequestDTO otpRequestDTO){
        OtpResponseDTO otpResponseDTO= otpService.generateOtp(otpRequestDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP is generated successfully", otpResponseDTO));
    }

    @PostMapping("/appointments/book")
    public ResponseEntity<ApiResponse<AppointmentDTO>> selfBookAppointment(@RequestBody PatientSelfBookingDTO requestDTO) {
        AppointmentDTO appointmentDTO = appointmentService.selfBookAppointment(requestDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment booked successfully", appointmentDTO));
    }


    @GetMapping("/app/runtime")
    public ResponseEntity<ApiResponse<RuntimeApplicationConfigDTO>> getRuntimeApplicationConfig(){
        RuntimeApplicationConfigDTO runtimeApplicationConfigDTO= runtimeApplicationConfigService.getRuntimeApplicationConfig();
        return ResponseEntity
                .ok(
                        new ApiResponse<>(
                                true,
                                "Runtime Application config Fetched successfully",
                                runtimeApplicationConfigDTO
                        )
                );
    }


}

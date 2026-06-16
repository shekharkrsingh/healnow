package com.heal.doctor.controllers;

import com.heal.doctor.dto.LoginResponseDTO;
import com.heal.doctor.dto.RogerRegistrationDTO;
import com.heal.doctor.services.IRogerService;
import com.heal.doctor.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/rogers")
@RequiredArgsConstructor
public class RogerPublicController {

    private final IRogerService rogerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> registerRoger(@RequestBody RogerRegistrationDTO rogerRegistrationDTO) {
        LoginResponseDTO loginResponseDTO = rogerService.registerRoger(rogerRegistrationDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Roger registered successfully", loginResponseDTO));
    }
}

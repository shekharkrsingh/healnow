package com.heal.doctor.controllers;

import com.heal.doctor.dto.DefaultDoctorSettingsDTO;
import com.heal.doctor.services.IDefaultDoctorSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/default-doctor-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
public class DefaultDoctorSettingsController {

    private final IDefaultDoctorSettingsService service;

    @GetMapping
    public ResponseEntity<DefaultDoctorSettingsDTO> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping
    public ResponseEntity<DefaultDoctorSettingsDTO> updateSettings(@Valid @RequestBody DefaultDoctorSettingsDTO settingsDTO) {
        return ResponseEntity.ok(service.updateSettings(settingsDTO));
    }
}

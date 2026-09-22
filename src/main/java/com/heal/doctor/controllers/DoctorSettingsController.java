package com.heal.doctor.controllers;

import com.heal.doctor.dto.DoctorSettingsDTO;
import com.heal.doctor.services.IDoctorSettingsService;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/doctor/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class DoctorSettingsController {

    private final IDoctorSettingsService service;

    @GetMapping
    public ResponseEntity<DoctorSettingsDTO> getDoctorSettings() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        return ResponseEntity.ok(service.getSettingsByDoctorId(doctorId));
    }

    @PutMapping
    public ResponseEntity<DoctorSettingsDTO> updateDoctorSettings(@Valid @RequestBody DoctorSettingsDTO settingsDTO) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        return ResponseEntity.ok(service.updateSettings(doctorId, settingsDTO));
    }
}

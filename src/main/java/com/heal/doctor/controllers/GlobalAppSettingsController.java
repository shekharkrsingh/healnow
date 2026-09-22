package com.heal.doctor.controllers;

import com.heal.doctor.dto.GlobalAppSettingsDTO;
import com.heal.doctor.services.IGlobalAppSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GlobalAppSettingsController {

    private final IGlobalAppSettingsService service;

    @GetMapping("/global-settings")
    public ResponseEntity<GlobalAppSettingsDTO> getGlobalSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping("/admin/global-settings")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalAppSettingsDTO> updateGlobalSettings(@Valid @RequestBody GlobalAppSettingsDTO settingsDTO) {
        return ResponseEntity.ok(service.updateSettings(settingsDTO));
    }
}

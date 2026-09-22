package com.heal.doctor.controllers;

import com.heal.doctor.dto.DefaultCollaboratorSettingsDTO;
import com.heal.doctor.services.IDefaultCollaboratorSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/default-collaborator-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COLLABORATOR')")
public class DefaultCollaboratorSettingsController {

    private final IDefaultCollaboratorSettingsService service;

    @GetMapping
    public ResponseEntity<DefaultCollaboratorSettingsDTO> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping
    public ResponseEntity<DefaultCollaboratorSettingsDTO> updateSettings(@Valid @RequestBody DefaultCollaboratorSettingsDTO settingsDTO) {
        return ResponseEntity.ok(service.updateSettings(settingsDTO));
    }
}

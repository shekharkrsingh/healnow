package com.heal.doctor.controllers;

import com.heal.doctor.dto.CollaboratorSettingsDTO;
import com.heal.doctor.services.ICollaboratorSettingsService;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/collaborator/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COLLABORATOR')")
public class CollaboratorSettingsController {

    private final ICollaboratorSettingsService service;

    @GetMapping
    public ResponseEntity<CollaboratorSettingsDTO> getCollaboratorSettings() {
        String collaboratorId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(service.getSettingsByCollaboratorId(collaboratorId));
    }

    @PutMapping
    public ResponseEntity<CollaboratorSettingsDTO> updateCollaboratorSettings(@Valid @RequestBody CollaboratorSettingsDTO settingsDTO) {
        String collaboratorId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(service.updateSettings(collaboratorId, settingsDTO));
    }
}

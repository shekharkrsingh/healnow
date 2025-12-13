package com.heal.doctor.controllers;

import com.heal.doctor.dto.DoctorDTO;
import com.heal.doctor.dto.UpdateCollaboratorProfileDTO;
import com.heal.doctor.services.ICollaboratorService;
import com.heal.doctor.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/collaborators/profile")
@PreAuthorize("hasRole('COLLABORATOR')")
public class CollaboratorProfileController {

    private final ICollaboratorService collaboratorService;

    @GetMapping
    public ResponseEntity<ApiResponse<DoctorDTO>> getCollaboratorProfile() {
        DoctorDTO profile = collaboratorService.getCollaboratorProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Collaborator profile retrieved successfully", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<DoctorDTO>> updateCollaboratorProfile(
            @Valid @RequestBody UpdateCollaboratorProfileDTO updateDTO) {
        DoctorDTO updatedProfile = collaboratorService.updateCollaboratorProfile(updateDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Collaborator profile updated successfully", updatedProfile));
    }
}

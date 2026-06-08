package com.heal.doctor.controllers;

import com.heal.doctor.dto.CollaboratorDTO;
import com.heal.doctor.services.ICollaboratorService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/collaborators")
@PreAuthorize("hasRole('DOCTOR')")
public class CollaboratorController {

    private final ICollaboratorService collaboratorService;

    public CollaboratorController(ICollaboratorService collaboratorService) {
        this.collaboratorService = collaboratorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollaboratorDTO>>> getCollaborators() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        List<CollaboratorDTO> collaborators = collaboratorService.getCollaboratorsByDoctor(doctorId);
        return ResponseEntity.ok(ApiResponse.<List<CollaboratorDTO>>builder()
                .success(true)
                .message("Collaborators fetched successfully")
                .data(collaborators)
                .build());
    }

    @PutMapping("/{collaboratorId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCollaborator(@PathVariable String collaboratorId) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        collaboratorService.deactivateCollaborator(collaboratorId, doctorId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Collaborator deactivated successfully")
                .build());
    }

    @PutMapping("/{collaboratorId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateCollaborator(@PathVariable String collaboratorId) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        collaboratorService.activateCollaborator(collaboratorId, doctorId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Collaborator activated successfully")
                .build());
    }

    @DeleteMapping("/{collaboratorId}")
    public ResponseEntity<ApiResponse<Void>> removeCollaborator(@PathVariable String collaboratorId) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        collaboratorService.removeCollaborator(collaboratorId, doctorId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Collaborator removed successfully")
                .build());
    }
}

package com.heal.doctor.controllers;

import com.heal.doctor.dto.AssociatedDoctorDTO;
import com.heal.doctor.dto.DoctorProfileDTO;
import com.heal.doctor.services.ICollaboratorDoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for multi-doctor collaborator operations.
 * Provides endpoints for listing associated doctors, switching active doctor,
 * and fetching the active doctor's full profile.
 */
@RestController
@RequestMapping("/api/v1/collaborators")
@RequiredArgsConstructor
public class CollaboratorDoctorController {

    private final ICollaboratorDoctorService collaboratorDoctorService;

    /**
     * List all doctors associated with the current collaborator.
     */
    @GetMapping("/doctors")
    public ResponseEntity<List<AssociatedDoctorDTO>> getAssociatedDoctors() {
        return ResponseEntity.ok(collaboratorDoctorService.getAssociatedDoctors());
    }

    /**
     * Switch the active doctor context for the current collaborator.
     */
    @PutMapping("/active-doctor/{doctorId}")
    public ResponseEntity<AssociatedDoctorDTO> switchActiveDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(collaboratorDoctorService.switchActiveDoctor(doctorId));
    }

    /**
     * Get the full profile of the active doctor (availability, verification, clinic info).
     * Used by the frontend to display the correct data when a collaborator is managing a doctor.
     */
    @GetMapping("/active-doctor/profile")
    public ResponseEntity<DoctorProfileDTO> getActiveDoctorProfile() {
        return ResponseEntity.ok(collaboratorDoctorService.getActiveDoctorProfile());
    }
}

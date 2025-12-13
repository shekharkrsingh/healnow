package com.heal.doctor.controllers;

import com.heal.doctor.dto.InvitationRequestDTO;
import com.heal.doctor.dto.InvitationResponseDTO;
import com.heal.doctor.dto.AcceptInvitationDTO;
import com.heal.doctor.services.IInvitationService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InvitationController {

    private final IInvitationService invitationService;

    public InvitationController(IInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/doctors/invitations")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<InvitationResponseDTO>> sendInvitation(
            @Valid @RequestBody InvitationRequestDTO requestDTO) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        InvitationResponseDTO invitation = invitationService.sendInvitation(doctorId, requestDTO);
        return ResponseEntity.ok(ApiResponse.<InvitationResponseDTO>builder()
                .success(true)
                .message("Invitation sent successfully")
                .data(invitation)
                .build());
    }

    @GetMapping("/doctors/invitations")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<InvitationResponseDTO>>> getInvitations() {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        List<InvitationResponseDTO> invitations = invitationService.getInvitationsByDoctor(doctorId);
        return ResponseEntity.ok(ApiResponse.<List<InvitationResponseDTO>>builder()
                .success(true)
                .message("Invitations fetched successfully")
                .data(invitations)
                .build());
    }

    @PostMapping("/public/invitations/accept")
    public ResponseEntity<ApiResponse<InvitationResponseDTO>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationDTO acceptDTO) {
        InvitationResponseDTO invitation = invitationService.acceptInvitation(acceptDTO);
        return ResponseEntity.ok(ApiResponse.<InvitationResponseDTO>builder()
                .success(true)
                .message("Invitation accepted successfully. You can now login with your credentials.")
                .data(invitation)
                .build());
    }

    @DeleteMapping("/doctors/invitations/{invitationId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Void>> revokeInvitation(@PathVariable String invitationId) {
        invitationService.revokeInvitation(invitationId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Invitation revoked successfully")
                .build());
    }
}

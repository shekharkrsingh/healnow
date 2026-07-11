package com.heal.doctor.controllers;

import com.heal.doctor.dto.AcceptAdminInviteDTO;
import com.heal.doctor.dto.AdminInviteRequestDTO;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.services.IAdminInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminInvitationController {

    private final IAdminInvitationService adminInvitationService;

    @PostMapping("/api/v1/admin/invitations/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> sendAdminInvitation(@Valid @RequestBody AdminInviteRequestDTO requestDTO) {
        adminInvitationService.sendInvitation(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Admin invitation sent successfully", null));
    }

    @PostMapping("/api/v1/public/admin/accept-invite")
    public ResponseEntity<ApiResponse<String>> acceptAdminInvitation(@Valid @RequestBody AcceptAdminInviteDTO acceptDTO) {
        adminInvitationService.acceptInvitation(acceptDTO);
        return ResponseEntity.ok(ApiResponse.success("Admin account created successfully", null));
    }
}

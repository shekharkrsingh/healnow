package com.heal.doctor.entity.controllers;

import com.heal.doctor.entity.dto.AffiliationDetailDTO;
import com.heal.doctor.entity.dto.RejectRequest;
import com.heal.doctor.entity.services.IAffiliationStateMachine;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/affiliations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAffiliationController {

    private final IAffiliationStateMachine stateMachine;

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> approve(@PathVariable String id) {
        String adminId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Affiliation approved", stateMachine.approveAdmin(id, adminId)));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> reject(
            @PathVariable String id, @Valid @RequestBody RejectRequest body) {
        String adminId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Affiliation rejected", stateMachine.rejectAdmin(id, adminId, body.getReason())));
    }

    @PutMapping("/{id}/reinstate")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> reinstate(@PathVariable String id) {
        String adminId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Affiliation reinstated", stateMachine.reinstate(id, adminId)));
    }
}

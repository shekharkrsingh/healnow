package com.heal.doctor.entity.controllers;

import com.heal.doctor.entity.dto.AffiliationDetailDTO;
import com.heal.doctor.entity.dto.AffiliationInitiateRequest;
import com.heal.doctor.entity.dto.AffiliationListDTO;
import com.heal.doctor.entity.dto.DataSharingPolicyUpdateRequest;
import com.heal.doctor.entity.dto.RejectRequest;
import com.heal.doctor.entity.dto.SuspendTerminateRequest;
import com.heal.doctor.entity.security.ContextResolutionFilter;
import com.heal.doctor.entity.security.EffectiveContext;
import com.heal.doctor.entity.services.IAffiliationService;
import com.heal.doctor.entity.services.IAffiliationStateMachine;
import com.heal.doctor.entity.services.IDataSharingService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/affiliations")
@RequiredArgsConstructor
public class AffiliationController {

    private final IAffiliationStateMachine stateMachine;
    private final IAffiliationService affiliationService;
    private final IDataSharingService dataSharingService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> initiate(
            @Valid @RequestBody AffiliationInitiateRequest request) {
        String userId = CurrentUserName.getCurrentUserId();
        AffiliationDetailDTO dto = stateMachine.initiate(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Affiliation initiated", dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AffiliationListDTO>>> listAffiliations(HttpServletRequest request) {
        EffectiveContext ctx = ContextResolutionFilter.fromRequest(request);
        return ResponseEntity.ok(ApiResponse.success(affiliationService.listAffiliations(ctx)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> getAffiliation(
            @PathVariable String id, HttpServletRequest request) {
        EffectiveContext ctx = ContextResolutionFilter.fromRequest(request);
        return ResponseEntity.ok(ApiResponse.success(affiliationService.getAffiliationDetail(id, ctx)));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> accept(@PathVariable String id) {
        String userId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Affiliation accepted", stateMachine.acceptPeer(id, userId)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> reject(
            @PathVariable String id, @Valid @RequestBody RejectRequest body) {
        String userId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Affiliation rejected", stateMachine.rejectPeer(id, userId, body.getReason())));
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> suspend(
            @PathVariable String id, @Valid @RequestBody SuspendTerminateRequest body) {
        String userId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Affiliation suspended", stateMachine.suspend(id, userId, body.getReason())));
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AffiliationDetailDTO>> terminate(
            @PathVariable String id, @Valid @RequestBody SuspendTerminateRequest body) {
        String userId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Affiliation terminated", stateMachine.terminate(id, userId, body.getReason())));
    }

    @PutMapping("/{id}/data-sharing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> updateDataSharing(
            @PathVariable String id, @Valid @RequestBody DataSharingPolicyUpdateRequest body) {
        String userId = CurrentUserName.getCurrentUserId();
        dataSharingService.updatePolicy(id, body, userId);
        return ResponseEntity.ok(ApiResponse.success("Data sharing policy updated", null));
    }
}

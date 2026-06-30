package com.heal.doctor.entity.controllers;

import com.heal.doctor.entity.dto.AssignStaffRequest;
import com.heal.doctor.entity.dto.StaffAssignmentDTO;
import com.heal.doctor.entity.security.ContextResolutionFilter;
import com.heal.doctor.entity.security.EffectiveContext;
import com.heal.doctor.entity.services.IStaffAssignmentService;
import com.heal.doctor.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/affiliations/{id}/staff")
@RequiredArgsConstructor
public class AffiliationStaffController {

    private final IStaffAssignmentService staffAssignmentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StaffAssignmentDTO>> assignStaff(
            @PathVariable String id,
            @Valid @RequestBody AssignStaffRequest request,
            HttpServletRequest httpRequest) {
        EffectiveContext ctx = ContextResolutionFilter.fromRequest(httpRequest);
        StaffAssignmentDTO dto = staffAssignmentService.assignStaff(id, request, ctx);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Staff assigned", dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StaffAssignmentDTO>>> listStaff(
            @PathVariable String id, HttpServletRequest httpRequest) {
        EffectiveContext ctx = ContextResolutionFilter.fromRequest(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(staffAssignmentService.listStaff(id, ctx)));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeStaff(
            @PathVariable String id,
            @PathVariable String userId,
            HttpServletRequest httpRequest) {
        EffectiveContext ctx = ContextResolutionFilter.fromRequest(httpRequest);
        staffAssignmentService.revokeStaff(id, userId, ctx);
        return ResponseEntity.ok(ApiResponse.success("Staff access revoked", null));
    }
}

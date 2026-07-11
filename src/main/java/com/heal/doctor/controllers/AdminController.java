package com.heal.doctor.controllers;

import com.heal.doctor.dto.AdminDashboardDTO;
import com.heal.doctor.dto.DoctorProfileDTO;
import com.heal.doctor.dto.CollaboratorProfileDTO;
import com.heal.doctor.dto.RuntimeApplicationConfigDTO;
import com.heal.doctor.services.IAdminDashboardService;
import com.heal.doctor.services.ICollaboratorService;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.IRuntimeApplicationConfigService;
import com.heal.doctor.models.enums.VerificationStatus;
import com.heal.doctor.utils.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IDoctorService doctorService;
    private final IRuntimeApplicationConfigService runtimeApplicationConfigService;
    private final IAdminDashboardService adminDashboardService;
    private final ICollaboratorService collaboratorService;

    @GetMapping
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<DoctorProfileDTO>>> getAllDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VerificationStatus status) {
        
        org.springframework.data.domain.Sort.Direction sortDirection = org.springframework.data.domain.Sort.Direction.fromString(direction);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(sortDirection, sortBy));
        
        org.springframework.data.domain.Page<DoctorProfileDTO> doctors = doctorService.getAllDoctorsPaginated(pageable, search, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", doctors));
    }


    @DeleteMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable String doctorId) {
        doctorService.deleteDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor deleted successfully", null));
    }

    @PutMapping("/{doctorId}/status")
    public ResponseEntity<ApiResponse<DoctorProfileDTO>> updateVerificationStatus(
            @PathVariable String doctorId,
            @RequestParam VerificationStatus status) {
        DoctorProfileDTO doctor = doctorService.updateVerificationStatus(doctorId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification status updated successfully", doctor));
    }


    @PostMapping("/app/runtime")
    public ResponseEntity<ApiResponse<RuntimeApplicationConfigDTO>> updateRuntimeApplicationConfig(
            @RequestBody RuntimeApplicationConfigDTO runtime
    ){
        RuntimeApplicationConfigDTO runtimeApplicationConfigDTO= runtimeApplicationConfigService.updateRuntimeApplicationConfig(runtime);
        return ResponseEntity
                .ok(
                        new ApiResponse<>(
                                true,
                                "Runtime Application config updated successfully",
                                runtimeApplicationConfigDTO
                        )
                );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDTO>> getDashboardAnalytics() {
        AdminDashboardDTO dashboard = adminDashboardService.getDashboardAnalytics();
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard analytics fetched successfully", dashboard));
    }

    @GetMapping("/collaborators")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CollaboratorProfileDTO>>> getAllCollaborators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.heal.doctor.models.enums.CollaboratorStatus status) {
        
        org.springframework.data.domain.Sort.Direction sortDirection = org.springframework.data.domain.Sort.Direction.fromString(direction);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(sortDirection, sortBy));
        
        org.springframework.data.domain.Page<CollaboratorProfileDTO> collaborators = collaboratorService.getAllCollaboratorsPaginated(pageable, search, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Collaborators retrieved successfully", collaborators));
    }
}

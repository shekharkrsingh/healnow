package com.heal.doctor.controllers;

import com.heal.doctor.dto.DoctorProfileDTO;
import com.heal.doctor.dto.RuntimeApplicationConfigDTO;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorProfileDTO>>> getAllDoctors() {
        List<DoctorProfileDTO> doctors = doctorService.getAllDoctors(null, null);
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
}

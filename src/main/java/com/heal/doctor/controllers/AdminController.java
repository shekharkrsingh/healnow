package com.heal.doctor.controllers;

import com.heal.doctor.dto.DoctorDTO;
import com.heal.doctor.dto.RuntimeApplicationConfigDTO;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.IRuntimeApplicationConfigService;
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
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getAllDoctors() {
        List<DoctorDTO> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", doctors));
    }

    @DeleteMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable String doctorId) {
        doctorService.deleteDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor deleted successfully", null));
    }

    @GetMapping("/app/runtime")
    public ResponseEntity<ApiResponse<RuntimeApplicationConfigDTO>> getRuntimeApplicationConfig(){
        RuntimeApplicationConfigDTO runtimeApplicationConfigDTO= runtimeApplicationConfigService.getRuntimeApplicationConfig();
        return ResponseEntity
                .ok(
                        new ApiResponse<>(
                                true,
                                "Runtime Application config Fetched successfully",
                                runtimeApplicationConfigDTO
                        )
                );
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

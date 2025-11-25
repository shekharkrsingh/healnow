package com.heal.doctor.controllers;

import com.heal.doctor.dto.DoctorDTO;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.utils.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Value("${app.version.minimum}")
    private String minimumVersion;

    @Value("${app.version.latest}")
    private String latestVersion;

    @Value("${app.version.forceUpdate}")
    private boolean forceUpdate;

    @Value("${app.version.websiteUrl}")
    private String websiteUrl;

    @Value("${app.version.message}")
    private String updateMessage;

    private final IDoctorService doctorService;

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

    @GetMapping("/app/version-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAppVersion() {
        if (minimumVersion == null || minimumVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("Minimum version is not configured");
        }

        if (websiteUrl == null || websiteUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Website URL is not configured");
        }

        Map<String, Object> versionData = new HashMap<>();
        versionData.put("minimumVersion", minimumVersion.trim());
        versionData.put("latestVersion", (latestVersion != null && !latestVersion.trim().isEmpty())
                ? latestVersion.trim()
                : minimumVersion.trim());
        versionData.put("forceUpdate", forceUpdate);
        versionData.put("websiteUrl", websiteUrl.trim());
        versionData.put("message", (updateMessage != null && !updateMessage.trim().isEmpty())
                ? updateMessage.trim()
                : String.format("A new version is available. Please update to version %s or higher to continue using the app.", minimumVersion));

        return ResponseEntity.ok(new ApiResponse<>(true, "Version check successful", versionData));
    }
}

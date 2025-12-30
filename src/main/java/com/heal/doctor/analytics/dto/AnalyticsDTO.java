package com.heal.doctor.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ========================================
 * ANALYTICS MODULE - DTO
 * ========================================
 * This DTO is part of the analytics tracking system.
 * It contains device metadata sent from the frontend.
 * 
 * TO REMOVE THIS MODULE:
 * 1. Delete the entire 'com.heal.doctor.analytics' package
 * 2. Remove analytics API calls from frontend
 * 3. Drop 'analytics' collection from MongoDB
 * ========================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDTO {

    @NotBlank(message = "Platform is required")
    @Size(max = 20, message = "Platform must not exceed 20 characters")
    @Pattern(regexp = "^(ios|android|web)$", message = "Platform must be ios, android, or web", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String platform;

    @NotBlank(message = "OS version is required")
    @Size(max = 50, message = "OS version must not exceed 50 characters")
    private String osVersion;

    @NotBlank(message = "App version is required")
    @Size(max = 20, message = "App version must not exceed 20 characters")
    private String appVersion;

    @Size(max = 50, message = "Device manufacturer must not exceed 50 characters")
    private String deviceManufacturer;

    @Size(max = 50, message = "Device model must not exceed 50 characters")
    private String deviceModel;

    @Size(max = 50, message = "Device brand must not exceed 50 characters")
    private String deviceBrand;

    @NotNull(message = "Screen width is required")
    private Integer screenWidth;

    @NotNull(message = "Screen height is required")
    private Integer screenHeight;

    @Size(max = 20, message = "Network type must not exceed 20 characters")
    private String networkType;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;
}

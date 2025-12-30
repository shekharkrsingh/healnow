package com.heal.doctor.analytics.controllers;

import com.heal.doctor.analytics.dto.AnalyticsDTO;
import com.heal.doctor.analytics.services.IAnalyticsService;
import com.heal.doctor.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ========================================
 * ANALYTICS MODULE - CONTROLLER
 * ========================================
 * Public REST endpoint for analytics tracking.
 * NO authentication required - anonymous tracking.
 * 
 * TO REMOVE THIS MODULE:
 * 1. Delete the entire 'com.heal.doctor.analytics' package
 * 2. Remove analytics API calls from frontend (fhpotion/analytics/)
 * 3. Drop 'analytics' collection from MongoDB: db.analytics.drop()
 * 
 * ENDPOINT: POST /api/v1/public/analytics/track
 * ========================================
 */
@RestController
@RequestMapping("/api/v1/public/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final IAnalyticsService analyticsService;

    /**
     * Track device analytics (anonymous)
     * 
     * @param analyticsDTO Device metadata from frontend
     * @param request HTTP request (for IP extraction)
     * @return Success response
     */
    @PostMapping("/track")
    public ResponseEntity<ApiResponse<Void>> trackAnalytics(
            @Valid @RequestBody AnalyticsDTO analyticsDTO,
            HttpServletRequest request) {
        
        log.debug("Received analytics tracking request from platform: {}", analyticsDTO.getPlatform());
        
        // Record analytics (service handles all errors internally)
        analyticsService.recordAnalytics(analyticsDTO, request);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true, 
                "Analytics tracked successfully", 
                null
        ));
    }
}

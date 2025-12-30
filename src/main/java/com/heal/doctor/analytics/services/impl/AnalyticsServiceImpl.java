package com.heal.doctor.analytics.services.impl;

import com.heal.doctor.analytics.dto.AnalyticsDTO;
import com.heal.doctor.analytics.models.AnalyticsEntity;
import com.heal.doctor.analytics.repositories.AnalyticsRepository;
import com.heal.doctor.analytics.services.GeoLocationService;
import com.heal.doctor.analytics.services.IAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * ========================================
 * ANALYTICS MODULE - SERVICE IMPLEMENTATION
 * ========================================
 * Implementation of analytics tracking service.
 * Extracts IP, performs geolocation, and saves to MongoDB.
 * 
 * TO REMOVE THIS MODULE:
 * Delete the entire 'com.heal.doctor.analytics' package
 * ========================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements IAnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final GeoLocationService geoLocationService;

    @Override
    public void recordAnalytics(AnalyticsDTO analyticsDTO, HttpServletRequest request) {
        try {
            // Extract IP address from request
            String ipAddress = extractIpAddress(request);
            log.debug("Recording analytics for IP: {}", ipAddress);

            // Get geolocation data from IP
            GeoLocationService.GeoLocationData geoData = geoLocationService.getLocationFromIP(ipAddress);

            // Build analytics entity
            AnalyticsEntity entity = AnalyticsEntity.builder()
                    // IP and geolocation
                    .ipAddress(ipAddress)
                    .country(geoData.getCountry())
                    .city(geoData.getCity())
                    .latitude(geoData.getLatitude())
                    .longitude(geoData.getLongitude())
                    
                    // Device information from DTO
                    .platform(analyticsDTO.getPlatform())
                    .osVersion(analyticsDTO.getOsVersion())
                    .appVersion(analyticsDTO.getAppVersion())
                    .deviceManufacturer(analyticsDTO.getDeviceManufacturer())
                    .deviceModel(analyticsDTO.getDeviceModel())
                    .deviceBrand(analyticsDTO.getDeviceBrand())
                    
                    // Screen and network info
                    .screenWidth(analyticsDTO.getScreenWidth())
                    .screenHeight(analyticsDTO.getScreenHeight())
                    .networkType(analyticsDTO.getNetworkType())
                    .timezone(analyticsDTO.getTimezone())
                    
                    // Timestamps
                    .timestamp(new Date())
                    .createdAt(new Date())
                    .build();

            // Save to MongoDB
            analyticsRepository.save(entity);
            log.info("Analytics recorded successfully for platform: {}, country: {}", 
                    analyticsDTO.getPlatform(), geoData.getCountry());

        } catch (Exception e) {
            // Log error but don't throw - analytics should never break the app
            log.error("Error recording analytics: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract IP address from HTTP request
     * Checks X-Forwarded-For and X-Real-IP headers first (for proxies/load balancers)
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }
}

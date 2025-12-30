package com.heal.doctor.analytics.services;

import com.heal.doctor.analytics.dto.AnalyticsDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ========================================
 * ANALYTICS MODULE - SERVICE INTERFACE
 * ========================================
 * Service interface for analytics tracking.
 * 
 * TO REMOVE THIS MODULE:
 * Delete the entire 'com.heal.doctor.analytics' package
 * ========================================
 */
public interface IAnalyticsService {

    /**
     * Record analytics data from device
     * 
     * @param analyticsDTO Device metadata from frontend
     * @param request HTTP request to extract IP address
     */
    void recordAnalytics(AnalyticsDTO analyticsDTO, HttpServletRequest request);
}

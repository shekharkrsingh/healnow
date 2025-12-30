package com.heal.doctor.analytics.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * ========================================
 * ANALYTICS MODULE - ENTITY
 * ========================================
 * This entity stores anonymous device analytics.
 * NO user identification (userId, doctorId, etc.)
 * 
 * TO REMOVE THIS MODULE:
 * 1. Delete the entire 'com.heal.doctor.analytics' package
 * 2. Remove analytics API calls from frontend
 * 3. Drop 'analytics' collection from MongoDB:
 *    db.analytics.drop()
 * ========================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "analytics")
public class AnalyticsEntity {

    @Id
    private String id;

    // IP and Geolocation data (extracted server-side)
    @Indexed
    private String ipAddress;
    
    private String country;
    private String city;
    private Double latitude;
    private Double longitude;

    // Device information (from frontend)
    private String platform;
    private String osVersion;
    private String appVersion;
    private String deviceManufacturer;
    private String deviceModel;
    private String deviceBrand;

    // Screen information
    private Integer screenWidth;
    private Integer screenHeight;

    // Network and timezone
    private String networkType;
    private String timezone;

    // Timestamps
    @Indexed
    private Date timestamp;
    
    private Date createdAt;
}

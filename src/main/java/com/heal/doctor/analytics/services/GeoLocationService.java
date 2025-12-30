package com.heal.doctor.analytics.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * ========================================
 * ANALYTICS MODULE - GEOLOCATION SERVICE
 * ========================================
 * Service for IP geolocation lookup using free API.
 * Uses ip-api.com (free, no API key required)
 * 
 * TO REMOVE THIS MODULE:
 * Delete the entire 'com.heal.doctor.analytics' package
 * ========================================
 */
@Service
@Slf4j
public class GeoLocationService {

    private static final String GEO_API_URL = "http://ip-api.com/json/";
    private final RestTemplate restTemplate;

    public GeoLocationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get geographic location data from IP address
     * Uses free ip-api.com service
     * 
     * @param ipAddress IP address to lookup
     * @return GeoLocationData with country, city, lat, lon
     */
    public GeoLocationData getLocationFromIP(String ipAddress) {
        try {
            // Skip localhost/private IPs
            if (ipAddress == null || ipAddress.isEmpty() || 
                ipAddress.equals("127.0.0.1") || 
                ipAddress.equals("0:0:0:0:0:0:0:1") ||
                ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") ||
                ipAddress.startsWith("172.")) {
                
                log.debug("Skipping geolocation for local/private IP: {}", ipAddress);
                return GeoLocationData.builder()
                    .country("Unknown")
                    .city("Unknown")
                    .latitude(0.0)
                    .longitude(0.0)
                    .build();
            }

            String url = GEO_API_URL + ipAddress + "?fields=status,country,city,lat,lon";
            IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);

            if (response != null && "success".equals(response.getStatus())) {
                return GeoLocationData.builder()
                    .country(response.getCountry())
                    .city(response.getCity())
                    .latitude(response.getLat())
                    .longitude(response.getLon())
                    .build();
            }

            log.warn("Geolocation lookup failed for IP: {}", ipAddress);
            return getDefaultLocation();

        } catch (Exception e) {
            log.error("Error during geolocation lookup for IP {}: {}", ipAddress, e.getMessage());
            return getDefaultLocation();
        }
    }

    private GeoLocationData getDefaultLocation() {
        return GeoLocationData.builder()
            .country("Unknown")
            .city("Unknown")
            .latitude(0.0)
            .longitude(0.0)
            .build();
    }

    /**
     * Response from ip-api.com
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class IpApiResponse {
        private String status;
        private String country;
        private String city;
        private Double lat;
        private Double lon;
    }

    /**
     * Geolocation data result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeoLocationData {
        private String country;
        private String city;
        private Double latitude;
        private Double longitude;
    }
}

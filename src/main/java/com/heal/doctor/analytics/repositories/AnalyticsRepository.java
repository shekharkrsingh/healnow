package com.heal.doctor.analytics.repositories;

import com.heal.doctor.analytics.models.AnalyticsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

/**
 * ========================================
 * ANALYTICS MODULE - REPOSITORY
 * ========================================
 * Repository for analytics data access.
 * 
 * TO REMOVE THIS MODULE:
 * Delete the entire 'com.heal.doctor.analytics' package
 * ========================================
 */
public interface AnalyticsRepository extends MongoRepository<AnalyticsEntity, String> {

    /**
     * Find analytics by country for geographic analysis
     */
    List<AnalyticsEntity> findByCountry(String country);

    /**
     * Find analytics by platform for platform analysis
     */
    List<AnalyticsEntity> findByPlatform(String platform);

    /**
     * Count analytics records between two dates for daily/weekly stats
     */
    Long countByTimestampBetween(Date start, Date end);
}

package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.SiteStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteStatisticsRepository extends MongoRepository<SiteStatistics, String> {
    Optional<SiteStatistics> findFirstByIsActiveTrue();
}

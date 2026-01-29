package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.SiteSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteSettingsRepository extends MongoRepository<SiteSettings, String> {
    Optional<SiteSettings> findFirstByIsActiveTrue();
}

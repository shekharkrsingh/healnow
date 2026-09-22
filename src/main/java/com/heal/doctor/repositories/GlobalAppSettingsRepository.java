package com.heal.doctor.repositories;

import com.heal.doctor.models.GlobalAppSettingsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalAppSettingsRepository extends MongoRepository<GlobalAppSettingsEntity, String> {
}

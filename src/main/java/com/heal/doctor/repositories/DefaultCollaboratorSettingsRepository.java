package com.heal.doctor.repositories;

import com.heal.doctor.models.DefaultCollaboratorSettingsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefaultCollaboratorSettingsRepository extends MongoRepository<DefaultCollaboratorSettingsEntity, String> {
}

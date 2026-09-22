package com.heal.doctor.repositories;

import com.heal.doctor.models.CollaboratorSettingsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollaboratorSettingsRepository extends MongoRepository<CollaboratorSettingsEntity, String> {
    Optional<CollaboratorSettingsEntity> findByCollaboratorId(String collaboratorId);
}

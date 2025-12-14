package com.heal.doctor.repositories;

import com.heal.doctor.models.CollaboratorProfileEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CollaboratorProfileRepository extends MongoRepository<CollaboratorProfileEntity, String> {
    Optional<CollaboratorProfileEntity> findByCollaboratorId(String collaboratorId);
    List<CollaboratorProfileEntity> findByDoctorId(String doctorId);
    boolean existsByCollaboratorId(String collaboratorId);
}

package com.heal.doctor.repositories;

import com.heal.doctor.models.OtpEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface OtpRepository extends MongoRepository<OtpEntity, Long> {
    Optional<OtpEntity> findTopByIdentifierOrderByCreatedAtDesc(String identifier);

    void deleteByIdentifier(String identifier);
    
    long countByIdentifier(String identifier);
}

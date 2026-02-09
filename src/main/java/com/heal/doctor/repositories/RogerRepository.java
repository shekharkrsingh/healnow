package com.heal.doctor.repositories;

import com.heal.doctor.models.RogerEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RogerRepository extends MongoRepository<RogerEntity, String> {
    Optional<RogerEntity> findByRogerId(String rogerId);
}

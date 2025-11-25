package com.heal.doctor.repositories;

import com.heal.doctor.models.RuntimeApplicationConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RuntimeApplicationConfigRepository extends MongoRepository<RuntimeApplicationConfig, String> {
}

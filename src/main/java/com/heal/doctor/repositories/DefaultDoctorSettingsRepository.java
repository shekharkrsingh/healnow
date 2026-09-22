package com.heal.doctor.repositories;

import com.heal.doctor.models.DefaultDoctorSettingsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefaultDoctorSettingsRepository extends MongoRepository<DefaultDoctorSettingsEntity, String> {
}

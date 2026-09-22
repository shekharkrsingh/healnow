package com.heal.doctor.repositories;

import com.heal.doctor.models.DoctorSettingsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorSettingsRepository extends MongoRepository<DoctorSettingsEntity, String> {
    Optional<DoctorSettingsEntity> findByDoctorId(String doctorId);
}

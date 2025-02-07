package com.heal.doctor.repositories;

import com.heal.doctor.models.DoctorEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DoctorRepository extends MongoRepository<DoctorEntity, String> {
    Optional<DoctorEntity> findByDoctorId(String doctorId);
    Optional<DoctorEntity> findByEmail(String email);
}

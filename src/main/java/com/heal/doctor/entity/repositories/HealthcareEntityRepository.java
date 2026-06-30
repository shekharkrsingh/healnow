package com.heal.doctor.entity.repositories;

import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.entity.models.enums.EntityStatus;
import com.heal.doctor.models.enums.VerificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthcareEntityRepository extends MongoRepository<HealthcareEntity, String> {

    Optional<HealthcareEntity> findByEntityId(String entityId);

    List<HealthcareEntity> findByStatus(EntityStatus status);

    List<HealthcareEntity> findByStatusAndVerificationStatus(EntityStatus status, VerificationStatus verificationStatus);

    List<HealthcareEntity> findByNameContainingIgnoreCaseAndStatus(String name, EntityStatus status);

    List<HealthcareEntity> findByCityIgnoreCaseAndStatus(String city, EntityStatus status);

    boolean existsByEntityId(String entityId);

    boolean existsByRegistrationNumber(String registrationNumber);
}

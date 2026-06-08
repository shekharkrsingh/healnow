package com.heal.doctor.repositories;

import com.heal.doctor.models.DoctorVerificationRequestEntity;
import com.heal.doctor.models.enums.RequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorVerificationRequestRepository extends MongoRepository<DoctorVerificationRequestEntity, String> {
    Optional<DoctorVerificationRequestEntity> findFirstByDoctorIdAndStatusOrderBySubmittedAtDesc(String doctorId, RequestStatus status);
}

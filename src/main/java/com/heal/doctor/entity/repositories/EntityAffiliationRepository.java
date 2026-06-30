package com.heal.doctor.entity.repositories;

import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityAffiliationRepository extends MongoRepository<EntityAffiliation, String> {

    Optional<EntityAffiliation> findByAffiliationId(String affiliationId);

    Optional<EntityAffiliation> findByEntityIdAndDoctorId(String entityId, String doctorId);

    List<EntityAffiliation> findByDoctorId(String doctorId);

    List<EntityAffiliation> findByDoctorIdAndStatus(String doctorId, AffiliationStatus status);

    List<EntityAffiliation> findByEntityId(String entityId);

    List<EntityAffiliation> findByEntityIdAndStatus(String entityId, AffiliationStatus status);

    Page<EntityAffiliation> findByStatusOrderByCreatedAtDesc(AffiliationStatus status, Pageable pageable);

    boolean existsByEntityIdAndDoctorId(String entityId, String doctorId);

    boolean existsByEntityIdAndDoctorIdAndStatusNot(String entityId, String doctorId, AffiliationStatus status);
}

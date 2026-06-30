package com.heal.doctor.entity.repositories;

import com.heal.doctor.entity.models.AffiliationStaffAssignment;
import com.heal.doctor.entity.models.enums.StaffAssignmentScope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AffiliationStaffRepository extends MongoRepository<AffiliationStaffAssignment, String> {

    List<AffiliationStaffAssignment> findByAffiliationIdAndActive(String affiliationId, boolean active);

    List<AffiliationStaffAssignment> findByAffiliationId(String affiliationId);

    List<AffiliationStaffAssignment> findByUserIdAndActive(String userId, boolean active);

    List<AffiliationStaffAssignment> findByEntityIdAndActive(String entityId, boolean active);

    List<AffiliationStaffAssignment> findByAffiliationIdAndScope(String affiliationId, StaffAssignmentScope scope);

    Optional<AffiliationStaffAssignment> findByAffiliationIdAndUserId(String affiliationId, String userId);

    boolean existsByAffiliationIdAndUserIdAndActive(String affiliationId, String userId, boolean active);
}

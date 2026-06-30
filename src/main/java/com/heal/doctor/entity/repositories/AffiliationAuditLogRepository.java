package com.heal.doctor.entity.repositories;

import com.heal.doctor.entity.models.AffiliationAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AffiliationAuditLogRepository extends MongoRepository<AffiliationAuditLog, String> {

    Page<AffiliationAuditLog> findByAffiliationIdOrderByTimestampDesc(String affiliationId, Pageable pageable);

    Page<AffiliationAuditLog> findByEntityIdOrderByTimestampDesc(String entityId, Pageable pageable);

    List<AffiliationAuditLog> findByActorUserIdAndTimestampBetween(String actorUserId, Instant from, Instant to);
}

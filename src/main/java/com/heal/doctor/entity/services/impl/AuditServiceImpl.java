package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.models.AffiliationAuditLog;
import com.heal.doctor.entity.repositories.AffiliationAuditLogRepository;
import com.heal.doctor.entity.services.IAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements IAuditService {

    private final AffiliationAuditLogRepository auditLogRepository;

    @Override
    public void log(String affiliationId, String entityId, String doctorId,
                    String actorUserId, String actorRole, String action,
                    Map<String, Object> metadata) {
        AffiliationAuditLog log = AffiliationAuditLog.builder()
                .affiliationId(affiliationId)
                .entityId(entityId)
                .doctorId(doctorId)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .action(action)
                .metadata(metadata)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(log);
    }
}

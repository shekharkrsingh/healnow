package com.heal.doctor.entity.services;

import java.util.Map;

public interface IAuditService {

    void log(String affiliationId, String entityId, String doctorId,
             String actorUserId, String actorRole, String action,
             Map<String, Object> metadata);
}

package com.heal.doctor.entity.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "affiliation_audit_logs")
@CompoundIndexes({
    @CompoundIndex(name = "affiliation_ts_idx", def = "{'affiliationId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "entity_ts_idx", def = "{'entityId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "actor_ts_idx", def = "{'actorUserId': 1, 'timestamp': -1}")
})
public class AffiliationAuditLog {

    @Id
    private String id;

    private String affiliationId;
    private String entityId;
    private String doctorId;
    private String actorUserId;
    private String actorRole;
    private String action;
    private Map<String, Object> metadata;
    private Instant timestamp;
}

package com.heal.doctor.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConflictCheckResult {

    private boolean hasConflicts;
    private List<ConflictDetail> conflicts;

    @Data
    @Builder
    public static class ConflictDetail {
        private String day;
        private String startTime;
        private String endTime;
        private String conflictingAffiliationId;
        private String conflictingEntityName;
    }
}

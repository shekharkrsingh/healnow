package com.heal.doctor.entity.security;

import com.heal.doctor.entity.models.enums.StaffAssignmentScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EffectiveContext {

    private String userId;
    private String primaryRole;

    private String doctorId;
    private String entityId;
    private String affiliationId;

    private Set<String> permissions;

    private StaffAssignmentScope staffScope;

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean isEntityScoped() {
        return entityId != null && !entityId.isBlank();
    }

    public boolean isAffiliationScoped() {
        return affiliationId != null && !affiliationId.isBlank();
    }

    public boolean isDoctorScoped() {
        return doctorId != null && !doctorId.isBlank();
    }
}

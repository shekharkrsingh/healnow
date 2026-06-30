package com.heal.doctor.entity.security;

import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.repositories.HealthcareEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class EntityPermissionEvaluator implements PermissionEvaluator {

    private final HealthcareEntityRepository healthcareEntityRepository;
    private final EntityAffiliationRepository affiliationRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) return false;
        String permissionStr = permission.toString();

        if (targetDomainObject instanceof HealthcareEntity entity) {
            return isEntityMemberWithPermission(authentication, entity.getEntityId(), permissionStr);
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || targetId == null || permission == null) return false;
        String permissionStr = permission.toString();

        return switch (targetType) {
            case "HealthcareEntity" -> isEntityMemberWithPermission(
                    authentication, targetId.toString(), permissionStr);
            case "EntityAffiliation" -> {
                var affOpt = affiliationRepository.findByAffiliationId(targetId.toString());
                yield affOpt.map(aff -> isEntityMemberWithPermission(
                        authentication, aff.getEntityId(), permissionStr)).orElse(false);
            }
            default -> false;
        };
    }

    private boolean isEntityMemberWithPermission(Authentication auth, String entityId, String permission) {
        String userId = resolveUserId(auth);
        if (userId == null) return false;

        return healthcareEntityRepository.findByEntityId(entityId)
                .map(entity -> entity.getMembers() != null && entity.getMembers().stream()
                        .filter(m -> m.getUserId().equals(userId) && m.isActive())
                        .anyMatch(m -> m.getPermissions() != null && m.getPermissions().contains(permission)))
                .orElse(false);
    }

    private String resolveUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof com.heal.doctor.security.DoctorUserDetails d) return d.getDoctorId();
        if (auth.getPrincipal() instanceof com.heal.doctor.security.CollaboratorUserDetails c) return c.getUserId();
        return null;
    }
}

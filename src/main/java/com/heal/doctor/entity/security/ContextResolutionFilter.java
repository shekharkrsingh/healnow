package com.heal.doctor.entity.security;

import com.heal.doctor.entity.models.AffiliationStaffAssignment;
import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import com.heal.doctor.entity.models.enums.EntityMemberRole;
import com.heal.doctor.entity.models.enums.EntityPermission;
import com.heal.doctor.entity.models.enums.StaffAssignmentScope;
import com.heal.doctor.entity.repositories.AffiliationStaffRepository;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.repositories.HealthcareEntityRepository;
import com.heal.doctor.security.CollaboratorUserDetails;
import com.heal.doctor.security.DoctorUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContextResolutionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ContextResolutionFilter.class);

    static final String EFFECTIVE_CONTEXT_ATTR = "effectiveContext";

    private final HealthcareEntityRepository healthcareEntityRepository;
    private final EntityAffiliationRepository affiliationRepository;
    private final AffiliationStaffRepository staffRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = resolveUserId(auth);
        String role = resolveRole(auth);
        if (userId == null || role == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String activeEntityId = request.getHeader("X-Active-Entity-Id");
        String activeAffiliationId = request.getHeader("X-Active-Affiliation-Id");

        EffectiveContext ctx = buildContext(userId, role, activeEntityId, activeAffiliationId, auth);
        request.setAttribute(EFFECTIVE_CONTEXT_ATTR, ctx);
        log.debug("EffectiveContext resolved: userId={}, role={}, entityId={}, affiliationId={}",
                userId, role, activeEntityId, activeAffiliationId);

        filterChain.doFilter(request, response);
    }

    private EffectiveContext buildContext(String userId, String role,
                                          String activeEntityId, String activeAffiliationId,
                                          Authentication auth) {
        EffectiveContext.EffectiveContextBuilder builder = EffectiveContext.builder()
                .userId(userId)
                .primaryRole(role);

        switch (role) {
            case "DOCTOR" -> {
                String doctorId = resolveDoctorId(auth);
                builder.doctorId(doctorId)
                        .permissions(defaultDoctorPermissions());
            }
            case "COLLABORATOR" -> {
                if (activeAffiliationId != null && !activeAffiliationId.isBlank()) {
                    Optional<AffiliationStaffAssignment> assignmentOpt =
                            staffRepository.findByAffiliationIdAndUserId(activeAffiliationId, userId);
                    assignmentOpt.ifPresent(assignment -> {
                        if (assignment.isActive()) {
                            builder.affiliationId(assignment.getAffiliationId())
                                    .entityId(assignment.getEntityId())
                                    .doctorId(assignment.getDoctorId())
                                    .staffScope(StaffAssignmentScope.ENTITY_CONTEXT)
                                    .permissions(new HashSet<>(assignment.getPermissions() != null
                                            ? assignment.getPermissions() : List.of()));
                        }
                    });
                } else {
                    String doctorId = resolveDoctorId(auth);
                    builder.doctorId(doctorId)
                            .staffScope(StaffAssignmentScope.DOCTOR_CONTEXT);
                }
            }
            case "ENTITY_ADMIN", "ENTITY_SUPERVISOR", "ENTITY_COLLABORATOR" -> {
                if (activeEntityId != null && !activeEntityId.isBlank()) {
                    Optional<HealthcareEntity> entityOpt =
                            healthcareEntityRepository.findByEntityId(activeEntityId);
                    entityOpt.ifPresent(entity -> {
                        entity.getMembers().stream()
                                .filter(m -> m.getUserId().equals(userId) && m.isActive())
                                .findFirst()
                                .ifPresent(member -> {
                                    Set<String> perms = resolveEntityMemberPermissions(member.getRole(),
                                            member.getPermissions());
                                    builder.entityId(activeEntityId).permissions(perms);
                                });
                    });

                    if (activeAffiliationId != null && !activeAffiliationId.isBlank()) {
                        Optional<EntityAffiliation> affOpt =
                                affiliationRepository.findByAffiliationId(activeAffiliationId);
                        affOpt.ifPresent(aff -> {
                            if (aff.getEntityId().equals(activeEntityId)) {
                                builder.affiliationId(activeAffiliationId)
                                        .doctorId(aff.getDoctorId());
                            }
                        });
                    }
                }
            }
            case "ADMIN" -> builder.permissions(adminPermissions());
        }

        return builder.build();
    }

    private String resolveUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof DoctorUserDetails d) return d.getDoctorId();
        if (auth.getPrincipal() instanceof CollaboratorUserDetails c) return c.getUserId();
        return null;
    }

    private String resolveDoctorId(Authentication auth) {
        if (auth.getCredentials() instanceof String s && !s.isBlank()) return s;
        if (auth.getPrincipal() instanceof DoctorUserDetails d) return d.getDoctorId();
        return null;
    }

    private String resolveRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse(null);
    }

    private Set<String> resolveEntityMemberPermissions(EntityMemberRole role, List<String> overrides) {
        Set<String> base = new HashSet<>(defaultPermissionsForRole(role));
        if (overrides != null && !overrides.isEmpty()) {
            base.addAll(overrides);
        }
        return base;
    }

    private Set<String> defaultPermissionsForRole(EntityMemberRole role) {
        return switch (role) {
            case ENTITY_ADMIN -> new HashSet<>(Arrays.stream(EntityPermission.values())
                    .map(Enum::name).collect(Collectors.toSet()));
            case SUPERVISOR -> new HashSet<>(Set.of(
                    EntityPermission.ENTITY_READ.name(),
                    EntityPermission.AFFILIATION_INITIATE.name(),
                    EntityPermission.AFFILIATION_ACCEPT.name(),
                    EntityPermission.AFFILIATION_VIEW.name(),
                    EntityPermission.AFFILIATION_MANAGE_AVAILABILITY.name(),
                    EntityPermission.AFFILIATION_TERMINATE.name(),
                    EntityPermission.STAFF_ASSIGN_ENTITY.name(),
                    EntityPermission.STAFF_REVOKE.name(),
                    EntityPermission.STAFF_VIEW.name(),
                    EntityPermission.APPOINTMENT_VIEW_ENTITY.name(),
                    EntityPermission.APPOINTMENT_MANAGE_ENTITY.name()
            ));
            case ENTITY_COLLABORATOR -> new HashSet<>(Set.of(
                    EntityPermission.AFFILIATION_VIEW.name(),
                    EntityPermission.APPOINTMENT_VIEW_ENTITY.name()
            ));
        };
    }

    private Set<String> defaultDoctorPermissions() {
        return new HashSet<>(Set.of(
                EntityPermission.AFFILIATION_INITIATE.name(),
                EntityPermission.AFFILIATION_ACCEPT.name(),
                EntityPermission.AFFILIATION_VIEW.name(),
                EntityPermission.AFFILIATION_MANAGE_AVAILABILITY.name(),
                EntityPermission.AFFILIATION_TERMINATE.name(),
                EntityPermission.STAFF_ASSIGN_DOCTOR.name(),
                EntityPermission.STAFF_VIEW.name(),
                EntityPermission.APPOINTMENT_VIEW_ENTITY.name(),
                EntityPermission.APPOINTMENT_MANAGE_ENTITY.name()
        ));
    }

    private Set<String> adminPermissions() {
        return new HashSet<>(Set.of(
                EntityPermission.AFFILIATION_ADMIN_APPROVE.name(),
                EntityPermission.AFFILIATION_VIEW.name(),
                EntityPermission.ENTITY_READ.name(),
                EntityPermission.STAFF_VIEW.name()
        ));
    }

    public static EffectiveContext fromRequest(HttpServletRequest request) {
        return (EffectiveContext) request.getAttribute(EFFECTIVE_CONTEXT_ATTR);
    }
}

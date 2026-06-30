package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.dto.ContextSwitchRequest;
import com.heal.doctor.entity.dto.UserContextOptionDTO;
import com.heal.doctor.entity.models.AffiliationStaffAssignment;
import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import com.heal.doctor.entity.repositories.AffiliationStaffRepository;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.repositories.HealthcareEntityRepository;
import com.heal.doctor.entity.security.ContextResolutionFilter;
import com.heal.doctor.entity.security.EffectiveContext;
import com.heal.doctor.entity.services.IContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContextServiceImpl implements IContextService {

    private final HealthcareEntityRepository entityRepository;
    private final EntityAffiliationRepository affiliationRepository;
    private final AffiliationStaffRepository staffRepository;

    @Override
    public EffectiveContext resolveContext(HttpServletRequest request) {
        return ContextResolutionFilter.fromRequest(request);
    }

    @Override
    public List<UserContextOptionDTO> getAvailableContexts(String userId) {
        List<UserContextOptionDTO> contexts = new ArrayList<>();

        List<HealthcareEntity> entities = entityRepository.findAll();
        entities.stream()
                .filter(e -> e.getMembers() != null && e.getMembers().stream()
                        .anyMatch(m -> m.getUserId().equals(userId) && m.isActive()))
                .forEach(e -> {
                    String role = e.getMembers().stream()
                            .filter(m -> m.getUserId().equals(userId) && m.isActive())
                            .map(m -> m.getRole().name())
                            .findFirst().orElse(null);

                    contexts.add(UserContextOptionDTO.builder()
                            .contextType("ENTITY")
                            .contextId(e.getEntityId())
                            .entityId(e.getEntityId())
                            .entityName(e.getName())
                            .displayName(e.getName())
                            .role(role)
                            .build());
                });

        List<AffiliationStaffAssignment> staffAssignments = staffRepository.findByUserIdAndActive(userId, true);
        staffAssignments.forEach(a -> {
            affiliationRepository.findByAffiliationId(a.getAffiliationId()).ifPresent(aff -> {
                contexts.add(UserContextOptionDTO.builder()
                        .contextType("AFFILIATION")
                        .contextId(a.getAffiliationId())
                        .affiliationId(a.getAffiliationId())
                        .entityId(a.getEntityId())
                        .doctorId(a.getDoctorId())
                        .doctorName(aff.getDoctorName())
                        .entityName(aff.getEntityName())
                        .displayName(aff.getEntityName() + " — " + aff.getDoctorName())
                        .role(a.getRole())
                        .build());
            });
        });

        return contexts;
    }

    @Override
    public void setActiveContext(String userId, ContextSwitchRequest request) {
        if (request.getEntityId() != null) {
            entityRepository.findByEntityId(request.getEntityId())
                    .filter(e -> e.getMembers() != null && e.getMembers().stream()
                            .anyMatch(m -> m.getUserId().equals(userId) && m.isActive()))
                    .orElseThrow(() -> new com.heal.doctor.exception.ForbiddenException(
                            "User is not a member of entity: " + request.getEntityId()));
        }

        if (request.getAffiliationId() != null) {
            staffRepository.findByAffiliationIdAndUserId(request.getAffiliationId(), userId)
                    .filter(com.heal.doctor.entity.models.AffiliationStaffAssignment::isActive)
                    .orElseThrow(() -> new com.heal.doctor.exception.ForbiddenException(
                            "No active staff assignment for affiliation: " + request.getAffiliationId()));
        }
    }
}

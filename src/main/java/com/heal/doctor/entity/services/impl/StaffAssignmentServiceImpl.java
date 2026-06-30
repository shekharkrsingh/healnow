package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.dto.AssignStaffRequest;
import com.heal.doctor.entity.dto.StaffAssignmentDTO;
import com.heal.doctor.entity.events.StaffAssignedEvent;
import com.heal.doctor.entity.events.StaffRevokedEvent;
import com.heal.doctor.entity.models.AffiliationStaffAssignment;
import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.repositories.AffiliationStaffRepository;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.security.EffectiveContext;
import com.heal.doctor.entity.services.IAuditService;
import com.heal.doctor.entity.services.IStaffAssignmentService;
import com.heal.doctor.exception.ForbiddenException;
import com.heal.doctor.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffAssignmentServiceImpl implements IStaffAssignmentService {

    private final AffiliationStaffRepository staffRepository;
    private final EntityAffiliationRepository affiliationRepository;
    private final IAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    @Override
    public StaffAssignmentDTO assignStaff(String affiliationId, AssignStaffRequest request, EffectiveContext ctx) {
        EntityAffiliation aff = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityAffiliation", affiliationId));

        if (staffRepository.existsByAffiliationIdAndUserIdAndActive(affiliationId, request.getUserId(), true)) {
            throw new IllegalStateException("User is already an active staff member on this affiliation");
        }

        AffiliationStaffAssignment assignment = AffiliationStaffAssignment.builder()
                .affiliationId(affiliationId)
                .entityId(aff.getEntityId())
                .doctorId(aff.getDoctorId())
                .userId(request.getUserId())
                .assignedBy(request.getAssignedBy())
                .assignedByUserId(ctx.getUserId())
                .scope(request.getScope())
                .role(request.getRole())
                .permissions(request.getPermissions())
                .active(true)
                .assignedAt(new Date())
                .build();

        AffiliationStaffAssignment saved = staffRepository.save(assignment);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                ctx.getUserId(), ctx.getPrimaryRole(), "STAFF_ASSIGNED",
                Map.of("userId", request.getUserId(), "scope", request.getScope()));

        eventPublisher.publishEvent(new StaffAssignedEvent(
                saved.getId(), affiliationId, aff.getEntityId(), aff.getDoctorId(),
                request.getUserId(), request.getScope()));

        return modelMapper.map(saved, StaffAssignmentDTO.class);
    }

    @Override
    public List<StaffAssignmentDTO> listStaff(String affiliationId, EffectiveContext ctx) {
        return staffRepository.findByAffiliationIdAndActive(affiliationId, true)
                .stream()
                .map(a -> modelMapper.map(a, StaffAssignmentDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void revokeStaff(String affiliationId, String userId, EffectiveContext ctx) {
        AffiliationStaffAssignment assignment = staffRepository
                .findByAffiliationIdAndUserId(affiliationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("AffiliationStaffAssignment", userId));

        if (!assignment.isActive()) {
            throw new IllegalStateException("Staff assignment is already inactive");
        }

        if (!ctx.getUserId().equals(assignment.getAssignedByUserId()) && !"ADMIN".equals(ctx.getPrimaryRole())) {
            throw new ForbiddenException("Only the assigner or admin can revoke a staff assignment");
        }

        assignment.setActive(false);
        assignment.setRevokedAt(new Date());
        assignment.setRevokedByUserId(ctx.getUserId());
        staffRepository.save(assignment);

        EntityAffiliation aff = affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
        String entityId = aff != null ? aff.getEntityId() : null;
        String doctorId = aff != null ? aff.getDoctorId() : null;

        auditService.log(affiliationId, entityId, doctorId,
                ctx.getUserId(), ctx.getPrimaryRole(), "STAFF_REVOKED", Map.of("userId", userId));

        eventPublisher.publishEvent(new StaffRevokedEvent(
                assignment.getId(), affiliationId, entityId, doctorId, userId));
    }

    @Override
    public void revokeAllForAffiliation(String affiliationId) {
        List<AffiliationStaffAssignment> active = staffRepository.findByAffiliationIdAndActive(affiliationId, true);
        Date now = new Date();
        active.forEach(a -> {
            a.setActive(false);
            a.setRevokedAt(now);
        });
        staffRepository.saveAll(active);
    }
}

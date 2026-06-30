package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.dto.AffiliationDetailDTO;
import com.heal.doctor.entity.dto.AffiliationInitiateRequest;
import com.heal.doctor.entity.events.AffiliationAdminApprovedEvent;
import com.heal.doctor.entity.events.AffiliationPeerAcceptedEvent;
import com.heal.doctor.entity.events.AffiliationRejectedEvent;
import com.heal.doctor.entity.events.AffiliationRequestedEvent;
import com.heal.doctor.entity.events.AffiliationTerminatedEvent;
import com.heal.doctor.entity.models.DataSharingPolicy;
import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.entity.models.enums.AffiliationInitiator;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.repositories.HealthcareEntityRepository;
import com.heal.doctor.entity.services.IAffiliationStateMachine;
import com.heal.doctor.entity.services.IAuditService;
import com.heal.doctor.entity.services.IStaffAssignmentService;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.enums.VerificationStatus;
import com.heal.doctor.repositories.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AffiliationStateMachineImpl implements IAffiliationStateMachine {

    private final EntityAffiliationRepository affiliationRepository;
    private final HealthcareEntityRepository entityRepository;
    private final DoctorRepository doctorRepository;
    private final IStaffAssignmentService staffAssignmentService;
    private final IAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    @Override
    public AffiliationDetailDTO initiate(AffiliationInitiateRequest request, String actorUserId) {
        if (affiliationRepository.existsByEntityIdAndDoctorIdAndStatusNot(
                request.getEntityId(), request.getDoctorId(), AffiliationStatus.TERMINATED)) {
            throw new IllegalStateException("AFFILIATION_DUPLICATE");
        }

        HealthcareEntity entity = entityRepository.findByEntityId(request.getEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareEntity", request.getEntityId()));

        DoctorEntity doctor = doctorRepository.findByDoctorId(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", request.getDoctorId()));

        DataSharingPolicy policy = request.getDataSharingPolicy() != null
                ? request.getDataSharingPolicy()
                : buildDefaultPolicy();

        String affiliationId = "aff_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        EntityAffiliation affiliation = EntityAffiliation.builder()
                .affiliationId(affiliationId)
                .entityId(request.getEntityId())
                .doctorId(request.getDoctorId())
                .initiatedBy(request.getInitiatedBy())
                .initiatedByUserId(actorUserId)
                .status(AffiliationStatus.PENDING_PEER_ACCEPT)
                .entityAvailability(request.getEntityAvailability())
                .dataSharingPolicy(policy)
                .department(request.getDepartment())
                .doctorName(doctor.getFirstName() + " " + doctor.getLastName())
                .doctorSpecialization(doctor.getSpecialization())
                .entityName(entity.getName())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        EntityAffiliation saved = affiliationRepository.save(affiliation);

        auditService.log(affiliationId, request.getEntityId(), request.getDoctorId(),
                actorUserId, null, "AFFILIATION_REQUESTED", Map.of("initiatedBy", request.getInitiatedBy()));

        eventPublisher.publishEvent(new AffiliationRequestedEvent(
                affiliationId, request.getEntityId(), request.getDoctorId(),
                request.getInitiatedBy(), actorUserId));

        return toDTO(saved);
    }

    @Override
    public AffiliationDetailDTO acceptPeer(String affiliationId, String actorUserId) {
        EntityAffiliation aff = findAndValidateState(affiliationId, AffiliationStatus.PENDING_PEER_ACCEPT);

        aff.setStatus(AffiliationStatus.PENDING_ADMIN_APPROVAL);
        aff.setPeerAcceptedAt(new Date());
        aff.setPeerAcceptedByUserId(actorUserId);
        aff.setUpdatedAt(new Date());

        EntityAffiliation saved = affiliationRepository.save(aff);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                actorUserId, null, "AFFILIATION_PEER_ACCEPTED", Map.of());

        eventPublisher.publishEvent(new AffiliationPeerAcceptedEvent(
                affiliationId, aff.getEntityId(), aff.getDoctorId(), actorUserId));

        return toDTO(saved);
    }

    @Override
    public AffiliationDetailDTO rejectPeer(String affiliationId, String actorUserId, String reason) {
        EntityAffiliation aff = findAndValidateState(affiliationId, AffiliationStatus.PENDING_PEER_ACCEPT);

        aff.setStatus(AffiliationStatus.REJECTED);
        aff.setRejectedAt(new Date());
        aff.setRejectionReason(reason);
        aff.setUpdatedAt(new Date());

        EntityAffiliation saved = affiliationRepository.save(aff);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                actorUserId, null, "AFFILIATION_PEER_REJECTED", Map.of("reason", reason));

        eventPublisher.publishEvent(new AffiliationRejectedEvent(
                affiliationId, aff.getEntityId(), aff.getDoctorId(), actorUserId, reason));

        return toDTO(saved);
    }

    @Override
    public AffiliationDetailDTO approveAdmin(String affiliationId, String adminUserId) {
        EntityAffiliation aff = findAndValidateState(affiliationId, AffiliationStatus.PENDING_ADMIN_APPROVAL);

        DoctorEntity doctor = doctorRepository.findByDoctorId(aff.getDoctorId()).orElseThrow(
                () -> new ResourceNotFoundException("Doctor", aff.getDoctorId()));

        HealthcareEntity entity = entityRepository.findByEntityId(aff.getEntityId()).orElseThrow(
                () -> new ResourceNotFoundException("HealthcareEntity", aff.getEntityId()));

        if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Doctor must be verified before admin approval");
        }
        if (entity.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Entity must be verified before admin approval");
        }

        aff.setStatus(AffiliationStatus.ACTIVE);
        aff.setAdminApprovedAt(new Date());
        aff.setAdminApprovedByUserId(adminUserId);
        aff.setUpdatedAt(new Date());

        EntityAffiliation saved = affiliationRepository.save(aff);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                adminUserId, "ADMIN", "AFFILIATION_ADMIN_APPROVED", Map.of());

        eventPublisher.publishEvent(new AffiliationAdminApprovedEvent(
                affiliationId, aff.getEntityId(), aff.getDoctorId(), adminUserId));

        return toDTO(saved);
    }

    @Override
    public AffiliationDetailDTO rejectAdmin(String affiliationId, String adminUserId, String reason) {
        EntityAffiliation aff = findAndValidateState(affiliationId, AffiliationStatus.PENDING_ADMIN_APPROVAL);

        aff.setStatus(AffiliationStatus.REJECTED);
        aff.setRejectedAt(new Date());
        aff.setRejectionReason(reason);
        aff.setUpdatedAt(new Date());

        EntityAffiliation saved = affiliationRepository.save(aff);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                adminUserId, "ADMIN", "AFFILIATION_ADMIN_REJECTED", Map.of("reason", reason));

        eventPublisher.publishEvent(new AffiliationRejectedEvent(
                affiliationId, aff.getEntityId(), aff.getDoctorId(), adminUserId, reason));

        return toDTO(saved);
    }

    @Override
    public AffiliationDetailDTO suspend(String affiliationId, String actorUserId, String reason) {
        EntityAffiliation aff = findAndValidateState(affiliationId, AffiliationStatus.ACTIVE);

        aff.setStatus(AffiliationStatus.SUSPENDED);
        aff.setSuspendedAt(new Date());
        aff.setSuspensionReason(reason);
        aff.setUpdatedAt(new Date());

        EntityAffiliation saved = affiliationRepository.save(aff);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                actorUserId, null, "AFFILIATION_SUSPENDED", Map.of("reason", reason));

        return toDTO(saved);
    }

    @Override
    public AffiliationDetailDTO reinstate(String affiliationId, String adminUserId) {
        EntityAffiliation aff = findAndValidateState(affiliationId, AffiliationStatus.SUSPENDED);

        aff.setStatus(AffiliationStatus.ACTIVE);
        aff.setReinstatedAt(new Date());
        aff.setUpdatedAt(new Date());

        EntityAffiliation saved = affiliationRepository.save(aff);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                adminUserId, "ADMIN", "AFFILIATION_REINSTATED", Map.of());

        return toDTO(saved);
    }

    @Override
    public AffiliationDetailDTO terminate(String affiliationId, String actorUserId, String reason) {
        EntityAffiliation aff = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityAffiliation", affiliationId));

        if (aff.getStatus() != AffiliationStatus.ACTIVE && aff.getStatus() != AffiliationStatus.SUSPENDED) {
            throw new IllegalStateException("AFFILIATION_INVALID_STATE");
        }

        aff.setStatus(AffiliationStatus.TERMINATED);
        aff.setTerminatedAt(new Date());
        aff.setTerminationReason(reason);
        aff.setUpdatedAt(new Date());

        EntityAffiliation saved = affiliationRepository.save(aff);

        staffAssignmentService.revokeAllForAffiliation(affiliationId);

        auditService.log(affiliationId, aff.getEntityId(), aff.getDoctorId(),
                actorUserId, null, "AFFILIATION_TERMINATED", Map.of("reason", reason));

        eventPublisher.publishEvent(new AffiliationTerminatedEvent(
                affiliationId, aff.getEntityId(), aff.getDoctorId(), actorUserId, reason));

        return toDTO(saved);
    }

    private EntityAffiliation findAndValidateState(String affiliationId, AffiliationStatus requiredStatus) {
        EntityAffiliation aff = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityAffiliation", affiliationId));
        if (aff.getStatus() != requiredStatus) {
            throw new IllegalStateException("AFFILIATION_INVALID_STATE");
        }
        return aff;
    }

    private DataSharingPolicy buildDefaultPolicy() {
        return DataSharingPolicy.builder()
                .version(1)
                .doctorSharedFields(java.util.Set.of("firstName", "lastName", "specialization",
                        "profilePicture", "licenseNumber", "verificationStatus"))
                .entitySharedFields(java.util.Set.of("name", "address", "city", "phoneNumber",
                        "departments", "logoUrl"))
                .agreedAt(new Date())
                .build();
    }

    private AffiliationDetailDTO toDTO(EntityAffiliation aff) {
        return modelMapper.map(aff, AffiliationDetailDTO.class);
    }
}

package com.heal.doctor.entity.listeners;

import com.heal.doctor.entity.events.AffiliationAdminApprovedEvent;
import com.heal.doctor.entity.events.AffiliationAvailabilityChangedEvent;
import com.heal.doctor.entity.events.AffiliationPeerAcceptedEvent;
import com.heal.doctor.entity.events.AffiliationRejectedEvent;
import com.heal.doctor.entity.events.AffiliationRequestedEvent;
import com.heal.doctor.entity.events.AffiliationTerminatedEvent;
import com.heal.doctor.entity.events.StaffAssignedEvent;
import com.heal.doctor.entity.events.StaffRevokedEvent;
import com.heal.doctor.entity.services.IAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AffiliationAuditListener {

    private final IAuditService auditService;

    @EventListener
    public void onAffiliationRequested(AffiliationRequestedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                event.initiatedByUserId(), null, "AFFILIATION_REQUESTED",
                Map.of("initiatedBy", event.initiatedBy()));
    }

    @EventListener
    public void onPeerAccepted(AffiliationPeerAcceptedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                event.acceptedByUserId(), null, "AFFILIATION_PEER_ACCEPTED", Map.of());
    }

    @EventListener
    public void onAdminApproved(AffiliationAdminApprovedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                event.adminUserId(), "ADMIN", "AFFILIATION_ADMIN_APPROVED", Map.of());
    }

    @EventListener
    public void onRejected(AffiliationRejectedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                event.rejectedByUserId(), null, "AFFILIATION_REJECTED",
                Map.of("reason", event.reason()));
    }

    @EventListener
    public void onTerminated(AffiliationTerminatedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                event.terminatedByUserId(), null, "AFFILIATION_TERMINATED",
                Map.of("reason", event.reason()));
    }

    @EventListener
    public void onAvailabilityChanged(AffiliationAvailabilityChangedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                event.changedByUserId(), null, "AVAILABILITY_UPDATED", Map.of());
    }

    @EventListener
    public void onStaffAssigned(StaffAssignedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                null, null, "STAFF_ASSIGNED",
                Map.of("userId", event.userId(), "scope", event.scope()));
    }

    @EventListener
    public void onStaffRevoked(StaffRevokedEvent event) {
        auditService.log(event.affiliationId(), event.entityId(), event.doctorId(),
                null, null, "STAFF_REVOKED", Map.of("userId", event.userId()));
    }
}

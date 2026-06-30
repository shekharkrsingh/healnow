package com.heal.doctor.entity.controllers;

import com.heal.doctor.entity.dto.AvailabilityUpdateRequest;
import com.heal.doctor.entity.dto.ConflictCheckResult;
import com.heal.doctor.entity.events.AffiliationAvailabilityChangedEvent;
import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.services.IAvailabilityConflictService;
import com.heal.doctor.entity.services.IAuditService;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.DayAvailability;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/affiliations/{id}/availability")
@RequiredArgsConstructor
public class AffiliationAvailabilityController {

    private final EntityAffiliationRepository affiliationRepository;
    private final IAvailabilityConflictService conflictService;
    private final IAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DayAvailability>>> getAvailability(@PathVariable String id) {
        EntityAffiliation aff = findOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(aff.getEntityAvailability()));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DayAvailability>>> updateAvailability(
            @PathVariable String id, @Valid @RequestBody AvailabilityUpdateRequest request) {
        String userId = CurrentUserName.getCurrentUserId();
        EntityAffiliation aff = findOrThrow(id);

        ConflictCheckResult conflicts = conflictService.checkConflicts(
                aff.getDoctorId(), id, request.getAvailability());
        if (conflicts.isHasConflicts()) {
            return ResponseEntity.status(409).body(ApiResponse.error("Availability conflicts detected", "AFFILIATION_CONFLICT"));
        }

        aff.setEntityAvailability(request.getAvailability());
        aff.setUpdatedAt(new Date());
        affiliationRepository.save(aff);

        auditService.log(id, aff.getEntityId(), aff.getDoctorId(), userId, null,
                "AVAILABILITY_UPDATED", Map.of("slotCount", request.getAvailability().size()));

        eventPublisher.publishEvent(new AffiliationAvailabilityChangedEvent(
                id, aff.getEntityId(), aff.getDoctorId(), userId));

        return ResponseEntity.ok(ApiResponse.success("Availability updated", aff.getEntityAvailability()));
    }

    @GetMapping("/conflicts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConflictCheckResult>> checkConflicts(@PathVariable String id) {
        EntityAffiliation aff = findOrThrow(id);
        ConflictCheckResult result = conflictService.checkConflicts(
                aff.getDoctorId(), id, aff.getEntityAvailability());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private EntityAffiliation findOrThrow(String affiliationId) {
        return affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityAffiliation", affiliationId));
    }
}

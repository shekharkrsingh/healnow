package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.dto.ConflictCheckResult;
import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.services.IAvailabilityConflictService;
import com.heal.doctor.models.DayAvailability;
import com.heal.doctor.models.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityConflictServiceImpl implements IAvailabilityConflictService {

    private final EntityAffiliationRepository affiliationRepository;

    @Override
    public ConflictCheckResult checkConflicts(String doctorId, String affiliationId, List<DayAvailability> proposed) {
        List<EntityAffiliation> activeAffiliations = affiliationRepository
                .findByDoctorIdAndStatus(doctorId, AffiliationStatus.ACTIVE)
                .stream()
                .filter(a -> !a.getAffiliationId().equals(affiliationId))
                .toList();

        List<ConflictCheckResult.ConflictDetail> conflicts = new ArrayList<>();

        for (DayAvailability proposedDay : proposed) {
            if (proposedDay.getSlots() == null) continue;

            for (EntityAffiliation other : activeAffiliations) {
                if (other.getEntityAvailability() == null) continue;

                other.getEntityAvailability().stream()
                        .filter(od -> od.getDay() == proposedDay.getDay())
                        .forEach(matchingDay -> {
                            if (matchingDay.getSlots() == null) return;
                            for (TimeSlot existingSlot : matchingDay.getSlots()) {
                                for (TimeSlot proposedSlot : proposedDay.getSlots()) {
                                    if (overlaps(existingSlot, proposedSlot)) {
                                        conflicts.add(ConflictCheckResult.ConflictDetail.builder()
                                                .day(proposedDay.getDay().name())
                                                .startTime(proposedSlot.getStartTime())
                                                .endTime(proposedSlot.getEndTime())
                                                .conflictingAffiliationId(other.getAffiliationId())
                                                .conflictingEntityName(other.getEntityName())
                                                .build());
                                    }
                                }
                            }
                        });
            }
        }

        return ConflictCheckResult.builder()
                .hasConflicts(!conflicts.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    @Override
    public ConflictCheckResult checkAllActiveAffiliations(String doctorId) {
        List<EntityAffiliation> activeAffiliations = affiliationRepository
                .findByDoctorIdAndStatus(doctorId, AffiliationStatus.ACTIVE);

        List<ConflictCheckResult.ConflictDetail> conflicts = new ArrayList<>();

        for (int i = 0; i < activeAffiliations.size(); i++) {
            EntityAffiliation a = activeAffiliations.get(i);
            if (a.getEntityAvailability() == null) continue;

            for (int j = i + 1; j < activeAffiliations.size(); j++) {
                EntityAffiliation b = activeAffiliations.get(j);
                if (b.getEntityAvailability() == null) continue;

                conflicts.addAll(detectConflictsBetween(a, b));
            }
        }

        return ConflictCheckResult.builder()
                .hasConflicts(!conflicts.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    private List<ConflictCheckResult.ConflictDetail> detectConflictsBetween(EntityAffiliation a, EntityAffiliation b) {
        List<ConflictCheckResult.ConflictDetail> result = new ArrayList<>();

        for (DayAvailability dayA : a.getEntityAvailability()) {
            if (dayA.getSlots() == null) continue;
            b.getEntityAvailability().stream()
                    .filter(dayB -> dayB.getDay() == dayA.getDay())
                    .forEach(dayB -> {
                        if (dayB.getSlots() == null) return;
                        for (TimeSlot slotA : dayA.getSlots()) {
                            for (TimeSlot slotB : dayB.getSlots()) {
                                if (overlaps(slotA, slotB)) {
                                    result.add(ConflictCheckResult.ConflictDetail.builder()
                                            .day(dayA.getDay().name())
                                            .startTime(slotA.getStartTime())
                                            .endTime(slotA.getEndTime())
                                            .conflictingAffiliationId(b.getAffiliationId())
                                            .conflictingEntityName(b.getEntityName())
                                            .build());
                                }
                            }
                        }
                    });
        }
        return result;
    }

    private boolean overlaps(TimeSlot a, TimeSlot b) {
        return a.getStartTime().compareTo(b.getEndTime()) < 0
                && b.getStartTime().compareTo(a.getEndTime()) < 0;
    }
}

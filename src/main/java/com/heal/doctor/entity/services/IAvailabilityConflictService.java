package com.heal.doctor.entity.services;

import com.heal.doctor.entity.dto.ConflictCheckResult;
import com.heal.doctor.models.DayAvailability;

import java.util.List;

public interface IAvailabilityConflictService {

    ConflictCheckResult checkConflicts(String doctorId, String affiliationId, List<DayAvailability> proposed);

    ConflictCheckResult checkAllActiveAffiliations(String doctorId);
}

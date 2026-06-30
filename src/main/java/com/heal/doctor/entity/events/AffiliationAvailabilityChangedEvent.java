package com.heal.doctor.entity.events;

public record AffiliationAvailabilityChangedEvent(String affiliationId, String entityId, String doctorId,
                                                   String changedByUserId) {}

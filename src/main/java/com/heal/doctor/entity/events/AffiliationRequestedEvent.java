package com.heal.doctor.entity.events;

import com.heal.doctor.entity.models.enums.AffiliationInitiator;

public record AffiliationRequestedEvent(String affiliationId, String entityId, String doctorId,
                                         AffiliationInitiator initiatedBy, String initiatedByUserId) {}

package com.heal.doctor.entity.events;

public record AffiliationTerminatedEvent(String affiliationId, String entityId, String doctorId,
                                          String terminatedByUserId, String reason) {}

package com.heal.doctor.entity.events;

public record AffiliationRejectedEvent(String affiliationId, String entityId, String doctorId,
                                        String rejectedByUserId, String reason) {}

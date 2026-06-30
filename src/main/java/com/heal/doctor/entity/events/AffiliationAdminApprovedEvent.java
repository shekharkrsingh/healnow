package com.heal.doctor.entity.events;

public record AffiliationAdminApprovedEvent(String affiliationId, String entityId, String doctorId,
                                             String adminUserId) {}

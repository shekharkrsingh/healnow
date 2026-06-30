package com.heal.doctor.entity.events;

public record StaffRevokedEvent(String assignmentId, String affiliationId, String entityId, String doctorId,
                                 String userId) {}

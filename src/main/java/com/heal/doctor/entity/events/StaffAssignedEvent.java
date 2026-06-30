package com.heal.doctor.entity.events;

import com.heal.doctor.entity.models.enums.StaffAssignmentScope;

public record StaffAssignedEvent(String assignmentId, String affiliationId, String entityId, String doctorId,
                                  String userId, StaffAssignmentScope scope) {}

package com.heal.doctor.entity.events;

public record AffiliationPeerAcceptedEvent(String affiliationId, String entityId, String doctorId,
                                            String acceptedByUserId) {}

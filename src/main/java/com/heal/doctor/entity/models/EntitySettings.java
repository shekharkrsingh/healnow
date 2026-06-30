package com.heal.doctor.entity.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntitySettings {

    private String timezone;

    @Builder.Default
    private AvailabilityEditMode availabilityEditMode = AvailabilityEditMode.DIRECT;

    private Integer maxBookingsPerSlot;

    @Builder.Default
    private boolean includeGlobalAvailabilityInConflicts = true;

    @Builder.Default
    private boolean emailNotificationsEnabled = true;

    @Builder.Default
    private boolean pushNotificationsEnabled = true;

    public enum AvailabilityEditMode {
        DIRECT,
        PROPOSE
    }
}

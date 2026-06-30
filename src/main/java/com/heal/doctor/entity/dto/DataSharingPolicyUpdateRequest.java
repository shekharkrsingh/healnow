package com.heal.doctor.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class DataSharingPolicyUpdateRequest {

    @NotNull
    private Set<String> doctorSharedFields;

    @NotNull
    private Set<String> entitySharedFields;

    private Map<String, Boolean> featureFlags;
}

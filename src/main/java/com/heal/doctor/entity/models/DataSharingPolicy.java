package com.heal.doctor.entity.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSharingPolicy {

    @Builder.Default
    private int version = 1;

    private Set<String> doctorSharedFields;

    private Set<String> entitySharedFields;

    private Map<String, Boolean> featureFlags;

    private Date agreedAt;
    private String agreedByDoctorUserId;
    private String agreedByEntityUserId;
}

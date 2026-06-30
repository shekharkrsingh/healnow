package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.DataSharingPolicy;
import com.heal.doctor.entity.models.enums.AffiliationInitiator;
import com.heal.doctor.models.DayAvailability;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AffiliationInitiateRequest {

    @NotBlank
    private String entityId;

    @NotBlank
    private String doctorId;

    @NotNull
    private AffiliationInitiator initiatedBy;

    private String department;

    private List<DayAvailability> entityAvailability;

    private DataSharingPolicy dataSharingPolicy;
}

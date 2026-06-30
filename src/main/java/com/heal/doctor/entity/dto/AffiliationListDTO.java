package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.enums.AffiliationStatus;
import lombok.Data;

import java.util.Date;

@Data
public class AffiliationListDTO {

    private String affiliationId;
    private String entityId;
    private String entityName;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String department;
    private AffiliationStatus status;
    private Date createdAt;
    private Date updatedAt;
}

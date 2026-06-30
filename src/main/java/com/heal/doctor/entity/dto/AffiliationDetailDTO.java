package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.DataSharingPolicy;
import com.heal.doctor.entity.models.enums.AffiliationInitiator;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import com.heal.doctor.models.DayAvailability;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AffiliationDetailDTO {

    private String affiliationId;
    private String entityId;
    private String entityName;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String department;

    private AffiliationInitiator initiatedBy;
    private String initiatedByUserId;
    private AffiliationStatus status;

    private List<DayAvailability> entityAvailability;
    private DataSharingPolicy dataSharingPolicy;

    private DoctorSharedProfileDTO doctorSharedProfile;
    private EntitySharedProfileDTO entitySharedProfile;

    private Date peerAcceptedAt;
    private String peerAcceptedByUserId;
    private Date adminApprovedAt;
    private String adminApprovedByUserId;
    private Date rejectedAt;
    private String rejectionReason;
    private Date suspendedAt;
    private String suspensionReason;
    private Date terminatedAt;
    private String terminationReason;

    private Date createdAt;
    private Date updatedAt;
}

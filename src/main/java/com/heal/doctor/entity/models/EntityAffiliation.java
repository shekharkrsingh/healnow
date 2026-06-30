package com.heal.doctor.entity.models;

import com.heal.doctor.entity.models.enums.AffiliationInitiator;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import com.heal.doctor.models.DayAvailability;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "entity_affiliations")
@CompoundIndexes({
    @CompoundIndex(name = "entity_doctor_unique", def = "{'entityId': 1, 'doctorId': 1}", unique = true),
    @CompoundIndex(name = "entity_status_idx", def = "{'entityId': 1, 'status': 1}"),
    @CompoundIndex(name = "doctor_status_idx", def = "{'doctorId': 1, 'status': 1}"),
    @CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': -1}")
})
public class EntityAffiliation {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "Affiliation ID is required")
    @Size(max = 50)
    private String affiliationId;

    @NotBlank(message = "Entity ID is required")
    private String entityId;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    private AffiliationInitiator initiatedBy;
    private String initiatedByUserId;

    @Builder.Default
    private AffiliationStatus status = AffiliationStatus.PENDING_PEER_ACCEPT;

    private List<DayAvailability> entityAvailability;

    private DataSharingPolicy dataSharingPolicy;

    private String doctorName;
    private String doctorSpecialization;
    private String entityName;
    private String department;

    private Date peerAcceptedAt;
    private String peerAcceptedByUserId;
    private Date adminApprovedAt;
    private String adminApprovedByUserId;
    private Date rejectedAt;
    private String rejectionReason;
    private Date suspendedAt;
    private String suspensionReason;
    private Date reinstatedAt;
    private Date terminatedAt;
    private String terminationReason;

    private Date createdAt;
    private Date updatedAt;
}

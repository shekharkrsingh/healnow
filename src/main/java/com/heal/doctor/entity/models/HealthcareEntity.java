package com.heal.doctor.entity.models;

import com.heal.doctor.entity.models.enums.EntityStatus;
import com.heal.doctor.entity.models.enums.EntityType;
import com.heal.doctor.models.enums.VerificationStatus;
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
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * First-class healthcare organization aggregate.
 * Replaces the flat clinic fields on DoctorEntity for affiliated doctors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "healthcare_entities")
@CompoundIndexes({
    @CompoundIndex(name = "status_verification_idx", def = "{'status': 1, 'verificationStatus': 1}"),
    @CompoundIndex(name = "name_city_idx", def = "{'name': 1, 'city': 1}")
})
public class HealthcareEntity {

    @Id
    private String id;

    /** Stable business ID, e.g. "ent_abc123". Generated on creation. */
    @Indexed(unique = true)
    @NotBlank(message = "Entity ID is required")
    @Size(max = 50, message = "Entity ID must not exceed 50 characters")
    private String entityId;

    @NotBlank(message = "Entity name is required")
    @Size(min = 2, max = 200, message = "Entity name must be between 2 and 200 characters")
    @TextIndexed
    private String name;

    private EntityType type;

    @Builder.Default
    private EntityStatus status = EntityStatus.DRAFT;

    // --- Registration & Compliance ---

    /** Government registration / business number. */
    @Size(max = 100)
    private String registrationNumber;

    /** URL to uploaded license document (S3 / cloud storage). */
    @Size(max = 500)
    private String licenseDocumentUrl;

    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    // --- Contact & Location ---

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    @TextIndexed
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 10)
    private String pincode;

    @Size(max = 15)
    private String phoneNumber;

    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String logoUrl;

    // --- Organization ---

    /** Departments hosted at this entity, e.g. ["Cardiology", "Orthopedics"]. */
    private List<String> departments;

    /** Members of this entity (admins, supervisors, collaborators). */
    private List<EntityMember> members;

    // --- Settings ---

    @Builder.Default
    private EntitySettings settings = new EntitySettings();

    // --- Audit ---

    /** userId of the user who registered this entity. */
    private String createdByUserId;

    private Date createdAt;
    private Date updatedAt;
}

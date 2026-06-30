package com.heal.doctor.entity.models;

import com.heal.doctor.entity.models.enums.StaffAssignmentScope;
import com.heal.doctor.entity.models.enums.StaffAssigner;
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
@Document(collection = "affiliation_staff")
@CompoundIndexes({
    @CompoundIndex(name = "affiliation_user_idx", def = "{'affiliationId': 1, 'userId': 1}", unique = true),
    @CompoundIndex(name = "user_active_idx", def = "{'userId': 1, 'active': 1}"),
    @CompoundIndex(name = "entity_active_idx", def = "{'entityId': 1, 'active': 1}")
})
public class AffiliationStaffAssignment {

    @Id
    private String id;

    @Indexed
    private String affiliationId;

    private String entityId;
    private String doctorId;
    private String userId;

    private StaffAssigner assignedBy;
    private String assignedByUserId;
    private StaffAssignmentScope scope;

    private String role;
    private List<String> permissions;

    @Builder.Default
    private boolean active = true;

    private Date assignedAt;
    private Date revokedAt;
    private String revokedByUserId;
}

package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.enums.StaffAssignmentScope;
import com.heal.doctor.entity.models.enums.StaffAssigner;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class StaffAssignmentDTO {

    private String id;
    private String affiliationId;
    private String entityId;
    private String doctorId;
    private String userId;
    private StaffAssigner assignedBy;
    private String assignedByUserId;
    private StaffAssignmentScope scope;
    private String role;
    private List<String> permissions;
    private boolean active;
    private Date assignedAt;
    private Date revokedAt;
}

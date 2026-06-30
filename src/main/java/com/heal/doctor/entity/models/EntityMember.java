package com.heal.doctor.entity.models;

import com.heal.doctor.entity.models.enums.EntityMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityMember {

    private String userId;
    private String displayName;
    private String email;
    private EntityMemberRole role;
    private List<String> permissions;
    private List<String> departmentIds;
    private Date joinedAt;

    @Builder.Default
    private boolean active = true;
}

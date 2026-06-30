package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.enums.EntityMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddMemberRequest {

    @NotBlank
    private String userId;

    @NotNull
    private EntityMemberRole role;

    @Email
    private String email;

    private String displayName;

    private List<String> permissions;

    private List<String> departmentIds;
}

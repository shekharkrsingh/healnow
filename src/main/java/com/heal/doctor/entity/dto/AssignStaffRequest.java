package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.enums.StaffAssignmentScope;
import com.heal.doctor.entity.models.enums.StaffAssigner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignStaffRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String email;

    private String firstName;
    private String lastName;

    @NotBlank
    private String role;

    @NotNull
    private StaffAssignmentScope scope;

    @NotNull
    private StaffAssigner assignedBy;

    private List<String> permissions;
}

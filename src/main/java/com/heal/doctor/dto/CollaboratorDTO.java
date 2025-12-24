package com.heal.doctor.dto;

import com.heal.doctor.models.enums.CollaboratorStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaboratorDTO {
    private String collaboratorId;
    private String doctorId;
    private String email;
    private String firstName;
    private String lastName;
    private CollaboratorStatus status;
    private Date createdAt;
}

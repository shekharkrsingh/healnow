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
public class CollaboratorProfileDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String collaboratorId;
    private String doctorId;
    private CollaboratorStatus status;
    private String profilePicture;
    private String coverPicture;
    private Date createdAt;
    private Date updatedAt;
}

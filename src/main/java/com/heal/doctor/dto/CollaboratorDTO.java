package com.heal.doctor.dto;

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
    private Boolean isActive;
    private Date createdAt;
}

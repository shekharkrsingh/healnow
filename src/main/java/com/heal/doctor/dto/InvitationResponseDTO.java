package com.heal.doctor.dto;

import com.heal.doctor.models.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponseDTO {
    private String invitationId;
    private String doctorId;
    private String email;
    private String firstName;
    private String lastName;
    private InvitationStatus status;
    private Date expiresAt;
    private Date acceptedAt;
    private Date createdAt;
}

package com.heal.doctor.models;

import com.heal.doctor.models.enums.InvitationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "admin_invitations")
public class AdminInvitationEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "Invitation ID is required")
    @Size(max = 50)
    private String invitationId;

    @Indexed(unique = true)
    @NotBlank(message = "Invitation token is required")
    @Size(max = 255)
    private String invitationToken;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$")
    private String lastName;

    @NotNull(message = "Status is required")
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @NotNull(message = "Expiration date is required")
    private Date expiresAt;

    private Date acceptedAt;
    private Date createdAt;
    private Date updatedAt;
}

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
@Document(collection = "invitations")
public class InvitationEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @UniqueElements
    @NotBlank(message = "Invitation ID is required")
    @Size(max = 50, message = "Invitation ID must not exceed 50 characters")
    private String invitationId;

    @Indexed(unique = true)
    @UniqueElements
    @NotBlank(message = "Invitation token is required")
    @Size(max = 255, message = "Invitation token must not exceed 255 characters")
    private String invitationToken;

    @Indexed(name = "doctor_id_idx")
    @NotBlank(message = "Doctor ID is required")
    @Size(max = 50, message = "Doctor ID must not exceed 50 characters")
    private String doctorId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
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

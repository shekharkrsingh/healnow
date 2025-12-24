package com.heal.doctor.models;

import com.heal.doctor.models.enums.CollaboratorStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Document(collection = "collaborator_profiles")
public class CollaboratorProfileEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @UniqueElements
    @NotBlank(message = "Collaborator ID is required")
    @Size(max = 50, message = "Collaborator ID must not exceed 50 characters")
    private String collaboratorId;

    @Indexed(name = "doctor_id_idx")
    @Size(max = 50, message = "Doctor ID must not exceed 50 characters")
    private String doctorId;

    private CollaboratorStatus status;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    private Date createdAt;
    private Date updatedAt;
}

package com.heal.doctor.models;

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
    @NotBlank(message = "Doctor ID is required")
    @Size(max = 50, message = "Doctor ID must not exceed 50 characters")
    private String doctorId;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    private String lastName;

    private Date createdAt;
    private Date updatedAt;
}

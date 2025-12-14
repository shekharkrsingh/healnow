package com.heal.doctor.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.heal.doctor.models.enums.RolesEnum;
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
@Document(collection = "users")
public class UserEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @UniqueElements
    @NotBlank(message = "User ID is required")
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;

    @Indexed(unique = true)
    @UniqueElements
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @JsonIgnore
    private String password;

    @NotNull(message = "Role is required")
    private RolesEnum rolesEnum;

    @NotNull(message = "Active status is required")
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean emailVerified = false;

    private Date createdAt;
    private Date updatedAt;
    private Date lastLoginAt;
}

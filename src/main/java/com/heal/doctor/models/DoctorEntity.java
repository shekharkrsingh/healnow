package com.heal.doctor.models;

import com.heal.doctor.models.enums.GenderEnum;
import com.heal.doctor.models.enums.VerificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "doctors")
public class DoctorEntity {

    private static final int VALID_PHONE_LENGTH = 10;
    private static final String PHONE_PATTERN = "^\\d{10}$";

    @Id
    private String id;

    @Indexed(unique = true)
    @UniqueElements
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

    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-,]+$", message = "Specialization can only contain letters, numbers, spaces, hyphens, and commas")
    private String specialization;

    @NotBlank(message = "Phone number is required")
    @Size(min = VALID_PHONE_LENGTH, max = VALID_PHONE_LENGTH, message = "Phone number must be exactly " + VALID_PHONE_LENGTH + " digits")
    @Pattern(regexp = PHONE_PATTERN, message = "Phone number must be exactly " + VALID_PHONE_LENGTH + " digits")
    private String phoneNumber;

    @Size(max = 7, message = "Availability entries cannot exceed 7 days")
    @Valid
    private List<DayAvailability> availability;

    @Size(max = 500, message = "Clinic address must not exceed 500 characters")
    private String clinicAddress;

    @Size(max = 100, message = "Clinic name must not exceed 100 characters")
    private String clinicName;

    @Size(max = 100, message = "Clinic email must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,6}$", message = "Invalid clinic email format")
    private String clinicEmail;

    @Size(min = VALID_PHONE_LENGTH, max = VALID_PHONE_LENGTH, message = "Clinic contact number must be exactly " + VALID_PHONE_LENGTH + " digits")
    @Pattern(regexp = PHONE_PATTERN, message = "Clinic contact number must be exactly " + VALID_PHONE_LENGTH + " digits")
    private String clinicContactNumber;

    @Valid
    private Address address;

    @Size(max = 20, message = "Education list cannot exceed 20 items")
    @Size(max = 200, message = "Each education entry must not exceed 200 characters")
    private List<String> education;

    @Size(max = 50, message = "Achievements and awards list cannot exceed 50 items")
    private List<String> achievementsAndAwards;

    @Size(max = 2000, message = "About section must not exceed 2000 characters")
    private String about;

    @Size(max = 5000, message = "Bio must not exceed 5000 characters")
    private String bio;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 70, message = "Years of experience cannot exceed 70")
    private Integer yearsOfExperience;

    private GenderEnum gender;

    @Size(max = 500, message = "Cover picture URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://.*|/.*|)$", message = "Cover picture must be a valid URL or file path")
    private String coverPicture;

    @Size(max = 500, message = "Profile picture URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://.*|/.*|)$", message = "Profile picture must be a valid URL or file path")
    private String profilePicture;

    @Size(max = 100, message = "License number must not exceed 100 characters")
    private String licenseNumber;

    @Size(max = 150, message = "Licensing authority must not exceed 150 characters")
    private String licensingAuthority;

    private Date licenseExpiryDate;

    private VerificationStatus verificationStatus;

    private Date createdAt;
    private Date updatedAt;
}

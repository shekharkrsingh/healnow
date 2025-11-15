package com.heal.doctor.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Data
@Document(collection = "otp")
@CompoundIndexes({
    @CompoundIndex(name = "identifier_created_idx", def = "{'identifier': 1, 'createdAt': -1}")
})
public class OtpEntity {
    @Id
    private String id;

    @Indexed(name = "identifier_idx")
    @NotBlank(message = "Identifier is required")
    @Email(message = "Identifier must be a valid email address")
    @Size(max = 255, message = "Identifier must not exceed 255 characters")
    private String identifier;

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 8, message = "OTP must be between 4 and 8 digits")
    @Pattern(regexp = "^\\d+$", message = "OTP must contain only digits")
    private String otp;

    @Indexed(name = "expiration_time_index", expireAfter = "0s")
    @NotNull(message = "Expiration time is required")
    private Date expirationTime;

    @Indexed(name = "created_at_idx")
    private Date createdAt;

    public OtpEntity() {
    }

    public OtpEntity(String identifier, String otp, Date expirationTime) {
        this.identifier = identifier;
        this.otp = otp;
        this.expirationTime = expirationTime;
        this.createdAt = new Date();
    }
}

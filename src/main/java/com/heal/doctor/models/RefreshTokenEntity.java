package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String tokenFamily;

    private String userId;
    private String email;
    private String role;
    private String doctorId;

    @Indexed(expireAfter = "0s")
    private Date expiresAt;

    private Date absoluteExpiresAt;

    private Date createdAt;

    private boolean revoked;
}

package com.heal.doctor.models;

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
    private String identifier;
    private String otp;

    @Indexed(name = "expiration_time_index", expireAfter = "0s")
    private Date expirationTime;

    @Indexed(name = "created_at_idx")
    private Date createdAt;

    public OtpEntity(String identifier, String otp, Date expirationTime) {
        this.identifier = identifier;
        this.otp = otp;
        this.expirationTime = expirationTime;
        this.createdAt = new Date();
    }
}

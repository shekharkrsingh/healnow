package com.heal.doctor.content.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "contact_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactInfo {

    @Id
    private String id;
    
    private String supportEmail;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String country;
    private String businessHours;
    private String responseTime;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}

package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "public_inquiries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicInquiryEntity {

    @Id
    private String id;
    
    private String name;
    private String email;
    private String phoneNumber;
    private String subject;
    private String message;
    
    @Builder.Default
    private String status = "PENDING";
    
    @CreatedDate
    private Instant createdAt;
}

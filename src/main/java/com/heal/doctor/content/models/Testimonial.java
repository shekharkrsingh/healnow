package com.heal.doctor.content.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "testimonials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Testimonial {

    @Id
    private String id;
    
    private String quote;
    private String author;
    private String role;
    private Integer rating;
    private String type;
    private Integer displayOrder;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}

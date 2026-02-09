package com.heal.doctor.content.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "hero_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroContent {

    @Id
    private String id;
    
    private String title;
    private String subtitle;
    private String videoUrl;
    private String ctaText;
    private String ctaLink;
    private String trustBadgeText;
    private Integer trustBadgeCount;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}

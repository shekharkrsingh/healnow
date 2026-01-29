package com.heal.doctor.content.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "onboarding_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStep {

    @Id
    private String id;
    
    private String title;
    private String description;
    private String iconName;
    private Integer stepNumber;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}

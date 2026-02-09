package com.heal.doctor.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStepDTO {
    private String id;
    private String title;
    private String description;
    private String iconName;
    private Integer stepNumber;
    private Boolean isActive;
}

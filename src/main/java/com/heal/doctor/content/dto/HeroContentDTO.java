package com.heal.doctor.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroContentDTO {
    private String title;
    private String subtitle;
    private String videoUrl;
    private String ctaText;
    private String ctaLink;
    private String trustBadgeText;
    private Integer trustBadgeCount;
}

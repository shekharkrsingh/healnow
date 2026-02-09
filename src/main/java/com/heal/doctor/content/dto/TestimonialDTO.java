package com.heal.doctor.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonialDTO {
    private String id;
    private String quote;
    private String author;
    private String role;
    private Integer rating;
    private String type;
    private Integer displayOrder;
    private Boolean isActive;
}

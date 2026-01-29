package com.heal.doctor.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactSubjectDTO {
    private String id;
    private String name;
    private Integer displayOrder;
    private Boolean isActive;
}

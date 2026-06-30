package com.heal.doctor.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EntitySharedProfileDTO {

    private Map<String, Object> fields;
}

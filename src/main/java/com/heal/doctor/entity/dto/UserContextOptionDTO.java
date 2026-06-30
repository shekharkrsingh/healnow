package com.heal.doctor.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserContextOptionDTO {

    private String contextType;
    private String contextId;
    private String displayName;
    private String entityId;
    private String entityName;
    private String affiliationId;
    private String doctorId;
    private String doctorName;
    private String role;
}

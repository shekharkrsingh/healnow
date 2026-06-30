package com.heal.doctor.entity.dto;

import lombok.Data;

@Data
public class ContextSwitchRequest {

    private String entityId;
    private String affiliationId;
    private String doctorId;
}

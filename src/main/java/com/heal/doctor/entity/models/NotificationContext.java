package com.heal.doctor.entity.models;

import com.heal.doctor.entity.models.enums.NotificationScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationContext {

    private String entityId;
    private String entityName;
    private String doctorId;
    private String doctorName;
    private String affiliationId;
    private NotificationScope scope;
}

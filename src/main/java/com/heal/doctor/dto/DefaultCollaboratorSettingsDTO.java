package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultCollaboratorSettingsDTO {
    private boolean enableNotifications;
    private boolean twoFactorAuthEnabled;
    private Date updatedAt;
}

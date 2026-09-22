package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaboratorSettingsDTO {
    @NotBlank(message = "Collaborator ID is required")
    private String collaboratorId;
    
    private boolean enableNotifications;
    private boolean twoFactorAuthEnabled;
    
    private Date updatedAt;
}

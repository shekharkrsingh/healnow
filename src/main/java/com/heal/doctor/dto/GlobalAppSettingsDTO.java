package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalAppSettingsDTO {
    private boolean maintenanceMode;
    private boolean allowNewRegistrations;
    
    @Email(message = "Contact email must be valid")
    private String contactEmail;
    
    private Date updatedAt;
}

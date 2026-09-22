package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.Email;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "global_app_settings")
public class GlobalAppSettingsEntity {

    public static final String SINGLETON_ID = "global_app_settings_config";

    @Id
    @Builder.Default
    private String id = SINGLETON_ID;

    private boolean maintenanceMode;

    private boolean allowNewRegistrations;

    @Email(message = "Contact email must be valid")
    private String contactEmail;

    private Date updatedAt;
}

package com.heal.doctor.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSettingsDTO {
    private String androidAppUrl;
    private String iosAppUrl;
    private String appVersion;
    private Boolean maintenanceMode;
    private String announcementText;
    private Boolean announcementActive;
}

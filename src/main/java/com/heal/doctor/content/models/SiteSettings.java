package com.heal.doctor.content.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "site_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSettings {

    @Id
    private String id;
    
    private String androidAppUrl;
    private String iosAppUrl;
    private String appVersion;
    
    @Builder.Default
    private Boolean maintenanceMode = false;
    
    private String announcementText;
    
    @Builder.Default
    private Boolean announcementActive = false;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}

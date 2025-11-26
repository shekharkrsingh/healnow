package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
public class RuntimeApplicationConfigDTO {

    private String id;
    private String minVersion;
    private String latestVersion;
    private String appWebUrl;
    private String googlePlayStoreUrl;
    private String applePlayStoreUrl;
    private String lastUpdatedBy;
    private String lastUpdatedById;
    private String appName;
    private String appSlogan;
    private String supportEmail;
    private Date updatedAt;
}

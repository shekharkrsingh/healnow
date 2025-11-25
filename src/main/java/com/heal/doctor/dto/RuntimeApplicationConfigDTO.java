package com.heal.doctor.dto;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Data
@Builder
@RequiredArgsConstructor
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

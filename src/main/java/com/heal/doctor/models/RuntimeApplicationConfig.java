package com.heal.doctor.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "runtime_application")
public class RuntimeApplicationConfig {

    public static final String SINGLETON_ID = "runtime_app_config";

    @Id
    @Builder.Default
    private String id = SINGLETON_ID;

    @NotBlank
    private String minVersion;

    @NotBlank
    private String latestVersion;

    @URL
    private String appWebUrl;

    @URL
    private String googlePlayStoreUrl;

    @URL
    private String applePlayStoreUrl;

    @NotBlank
    private String lastUpdatedBy;

    @NotBlank
    private String lastUpdatedById;

    @NotBlank
    private String appName;

    private String appSlogan;

    @Email
    @NotBlank
    private String supportEmail;

    @NotNull
    private Date updatedAt;
}

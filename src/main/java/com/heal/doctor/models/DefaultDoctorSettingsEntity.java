package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "default_doctor_settings")
public class DefaultDoctorSettingsEntity {

    public static final String SINGLETON_ID = "default_doctor_settings_config";

    @Id
    @Builder.Default
    private String id = SINGLETON_ID;

    private boolean publicBookingAllowed;
    private boolean enableEmergencyFeature;

    private Date updatedAt;
}

package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "doctor_settings")
public class DoctorSettingsEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    private boolean publicBookingAllowed;

    private boolean enableEmergencyFeature;

    private Date updatedAt;
}

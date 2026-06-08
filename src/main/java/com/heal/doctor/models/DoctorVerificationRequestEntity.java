package com.heal.doctor.models;

import com.heal.doctor.models.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "doctor_verification_requests")
public class DoctorVerificationRequestEntity {
    @Id
    private String id;

    @Indexed
    private String doctorId;

    private String licenseNumber;
    private String licensingAuthority;
    private Date licenseExpiryDate;

    private RequestStatus status;
    private Date submittedAt;
    private Date reviewedAt;
}

package com.heal.doctor.dto;


import com.heal.doctor.models.Address;
import com.heal.doctor.models.DayAvailability;
import com.heal.doctor.models.enums.GenderEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfileDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String doctorId;
    private String specialization;
    private String phoneNumber;
    private List<DayAvailability> availability;
    private String clinicAddress;
    private String clinicName;
    private String clinicEmail;
    private String clinicContactNumber;
    private Address address;
    private List<String> education;
    private List<String> achievementsAndAwards;
    private String about;
    private String bio;
    private Integer yearsOfExperience;
    private GenderEnum gender;
    private String coverPicture;
    private String profilePicture;
    private String licenseNumber;
    private String licensingAuthority;
    private Date licenseExpiryDate;
    private String verificationStatus;
    
    private boolean hasPendingVerification;
    private String pendingLicenseNumber;
    private String pendingLicensingAuthority;
    private Date pendingLicenseExpiryDate;

    private Date createdAt;
    private Date updatedAt;
}

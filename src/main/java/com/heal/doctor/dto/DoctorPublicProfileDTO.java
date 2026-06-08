package com.heal.doctor.dto;

import com.heal.doctor.models.Address;
import com.heal.doctor.models.DayAvailability;
import com.heal.doctor.models.enums.GenderEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorPublicProfileDTO {
    private String doctorId;
    private String firstName;
    private String lastName;
    private String specialization;
    private String clinicName;
    private String clinicEmail;
    private String clinicContactNumber;
    private String clinicAddress;
    private Address address; // Keeping this if it refers to clinic address, otherwise might need check
    private String about;
    private String bio;
    private Integer yearsOfExperience;
    private String profilePicture;
    private GenderEnum gender;
    private List<DayAvailability> availability;
    private List<String> education;
    private List<String> achievementsAndAwards;
}

package com.heal.doctor.dto;

import com.heal.doctor.models.Address;
import com.heal.doctor.models.DayAvailability;
import com.heal.doctor.models.enums.GenderEnum;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UpdateDoctorDetailsDTO {
    
    private static final int VALID_PHONE_LENGTH = 10;
    private static final String PHONE_PATTERN = "^\\d{10}$";
    
    private String firstName;
    private String lastName;
    private String specialization;
    
    @Size(min = VALID_PHONE_LENGTH, max = VALID_PHONE_LENGTH, message = "Phone number must be exactly " + VALID_PHONE_LENGTH + " digits")
    @Pattern(regexp = PHONE_PATTERN, message = "Phone number must be exactly " + VALID_PHONE_LENGTH + " digits")
    private String phoneNumber;
    private List<DayAvailability> availability;
    private String clinicAddress;
    private String clinicName;
    private String clinicEmail;
    
    @Size(min = VALID_PHONE_LENGTH, max = VALID_PHONE_LENGTH, message = "Clinic contact number must be exactly " + VALID_PHONE_LENGTH + " digits")
    @Pattern(regexp = PHONE_PATTERN, message = "Clinic contact number must be exactly " + VALID_PHONE_LENGTH + " digits")
    private String clinicContactNumber;
    private Address address;
    private List<String> education;
    private List<String> achievementsAndAwards;
    private String about;
    private String bio;
    private Integer yearsOfExperience;
    private GenderEnum gender;
    
    @Size(max = 100, message = "License number must not exceed 100 characters")
    private String licenseNumber;

    @Size(max = 150, message = "Licensing authority must not exceed 150 characters")
    private String licensingAuthority;

    private Date licenseExpiryDate;

}

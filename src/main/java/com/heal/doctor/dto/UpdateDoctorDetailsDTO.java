package com.heal.doctor.dto;

import com.heal.doctor.models.Address;
import com.heal.doctor.models.TimeSlot;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.enums.GenderEnum;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
    private List<AvailableDayEnum> availableDays;
    private List<TimeSlot> availableTimeSlots;
    private String clinicAddress;
    private Address address;
    private List<String> education;
    private List<String> achievementsAndAwards;
    private String about;
    private String bio;
    private Integer yearsOfExperience;
    private GenderEnum gender;
    private String coverPicture;
    private String profilePicture;

}

package com.heal.doctor.dto;

import com.heal.doctor.models.Address;
import com.heal.doctor.models.TimeSlot;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.enums.GenderEnum;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UpdateDoctorDetailsDTO {
    private String firstName;
    private String lastName;
    private String specialization;
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

}

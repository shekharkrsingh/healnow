package com.heal.doctor.dto;


import com.heal.doctor.models.Address;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.TimeSlot;
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
public class DoctorDTO {
    private String firstName;
    private String lastName;
    private String doctorId;
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
    private Date createdAt;
    private Date updatedAt;
}

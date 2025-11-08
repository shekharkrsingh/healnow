package com.heal.doctor.models;

import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.enums.GenderEnum;
import com.heal.doctor.models.enums.RolesEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "doctors")
public class DoctorEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @UniqueElements
    private String doctorId;

    @Indexed(unique = true)
    @UniqueElements
    private String email;

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

    private String password;

    private RolesEnum rolesEnum;

    private String coverPicture;
    private String profilePicture;

    private Date createdAt;
    private Date updatedAt;
}

package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RogerProfileDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String rogerId;
    private String phoneNumber;
    private String profilePicture;
    private String address;
    private Date createdAt;
    private Date updatedAt;
}

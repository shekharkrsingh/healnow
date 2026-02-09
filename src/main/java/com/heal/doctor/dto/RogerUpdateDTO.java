package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RogerUpdateDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePicture;
    private String address;
}

package com.heal.doctor.services;

import com.heal.doctor.dto.RogerProfileDTO;
import com.heal.doctor.dto.RogerRegistrationDTO;

public interface IRogerService {
    String registerRoger(RogerRegistrationDTO rogerRegistrationDTO);
    RogerProfileDTO getRogerProfile();
    RogerProfileDTO updateRogerProfile(com.heal.doctor.dto.RogerUpdateDTO updateDTO);
}

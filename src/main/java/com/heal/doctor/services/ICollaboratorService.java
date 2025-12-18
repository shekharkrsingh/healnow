package com.heal.doctor.services;

import com.heal.doctor.dto.CollaboratorDTO;
import com.heal.doctor.dto.UserDTO;
import com.heal.doctor.dto.UpdateCollaboratorProfileDTO;

import java.util.List;

public interface ICollaboratorService {
    List<CollaboratorDTO> getCollaboratorsByDoctor(String doctorId);
    void deactivateCollaborator(String collaboratorId);
    void activateCollaborator(String collaboratorId);
    void removeCollaborator(String collaboratorId);
    UserDTO getCollaboratorProfile();
    UserDTO updateCollaboratorProfile(UpdateCollaboratorProfileDTO updateDTO);
}

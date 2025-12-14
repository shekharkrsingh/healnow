package com.heal.doctor.services;

import com.heal.doctor.dto.AcceptInvitationDTO;
import com.heal.doctor.dto.InvitationRequestDTO;
import com.heal.doctor.dto.InvitationResponseDTO;

import java.util.List;

public interface IInvitationService {
    InvitationResponseDTO sendInvitation(String doctorId, InvitationRequestDTO requestDTO);
    InvitationResponseDTO acceptInvitation(AcceptInvitationDTO acceptDTO);
    List<InvitationResponseDTO> getInvitationsByDoctor(String doctorId);
    void revokeInvitation(String invitationId);
    boolean validateInvitationToken(String token);
}

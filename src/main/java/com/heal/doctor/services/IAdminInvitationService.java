package com.heal.doctor.services;

import com.heal.doctor.dto.AcceptAdminInviteDTO;
import com.heal.doctor.dto.AdminInviteRequestDTO;
import com.heal.doctor.models.AdminInvitationEntity;

import java.util.List;

public interface IAdminInvitationService {
    AdminInvitationEntity sendInvitation(AdminInviteRequestDTO requestDTO);
    void acceptInvitation(AcceptAdminInviteDTO acceptDTO);
    List<AdminInvitationEntity> getAllPendingInvitations();
}

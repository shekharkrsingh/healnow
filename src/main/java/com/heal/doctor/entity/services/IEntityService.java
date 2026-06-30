package com.heal.doctor.entity.services;

import com.heal.doctor.entity.dto.AddMemberRequest;
import com.heal.doctor.entity.dto.CreateEntityRequest;
import com.heal.doctor.entity.dto.EntityProfileDTO;
import com.heal.doctor.entity.dto.UpdateEntityRequest;
import com.heal.doctor.entity.models.enums.EntityMemberRole;

import java.util.List;

public interface IEntityService {

    EntityProfileDTO createEntity(CreateEntityRequest request, String createdByUserId);

    EntityProfileDTO getEntityById(String entityId);

    EntityProfileDTO updateEntity(String entityId, UpdateEntityRequest request, String actorUserId);

    List<EntityProfileDTO> searchEntities(String name, String city);

    EntityProfileDTO addMember(String entityId, AddMemberRequest request, String actorUserId);

    EntityProfileDTO updateMemberRole(String entityId, String userId, EntityMemberRole newRole, String actorUserId);

    EntityProfileDTO removeMember(String entityId, String userId, String actorUserId);

    List<EntityProfileDTO> getEntityMembers(String entityId);
}

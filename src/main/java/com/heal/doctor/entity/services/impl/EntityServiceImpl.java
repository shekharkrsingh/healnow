package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.dto.AddMemberRequest;
import com.heal.doctor.entity.dto.CreateEntityRequest;
import com.heal.doctor.entity.dto.EntityProfileDTO;
import com.heal.doctor.entity.dto.UpdateEntityRequest;
import com.heal.doctor.entity.models.EntityMember;
import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.entity.models.enums.EntityMemberRole;
import com.heal.doctor.entity.models.enums.EntityStatus;
import com.heal.doctor.entity.repositories.HealthcareEntityRepository;
import com.heal.doctor.entity.services.IEntityService;
import com.heal.doctor.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntityServiceImpl implements IEntityService {

    private final HealthcareEntityRepository entityRepository;
    private final ModelMapper modelMapper;

    @Override
    public EntityProfileDTO createEntity(CreateEntityRequest request, String createdByUserId) {
        String entityId = "ent_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        HealthcareEntity entity = HealthcareEntity.builder()
                .entityId(entityId)
                .name(request.getName())
                .type(request.getType())
                .status(EntityStatus.DRAFT)
                .registrationNumber(request.getRegistrationNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .departments(request.getDepartments())
                .members(new ArrayList<>())
                .createdByUserId(createdByUserId)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        EntityMember adminMember = EntityMember.builder()
                .userId(createdByUserId)
                .role(EntityMemberRole.ENTITY_ADMIN)
                .joinedAt(new Date())
                .active(true)
                .build();
        entity.getMembers().add(adminMember);

        HealthcareEntity saved = entityRepository.save(entity);
        return toDTO(saved);
    }

    @Override
    public EntityProfileDTO getEntityById(String entityId) {
        return toDTO(findOrThrow(entityId));
    }

    @Override
    public EntityProfileDTO updateEntity(String entityId, UpdateEntityRequest request, String actorUserId) {
        HealthcareEntity entity = findOrThrow(entityId);

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getType() != null) entity.setType(request.getType());
        if (request.getAddress() != null) entity.setAddress(request.getAddress());
        if (request.getCity() != null) entity.setCity(request.getCity());
        if (request.getState() != null) entity.setState(request.getState());
        if (request.getPincode() != null) entity.setPincode(request.getPincode());
        if (request.getPhoneNumber() != null) entity.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) entity.setEmail(request.getEmail());
        if (request.getLogoUrl() != null) entity.setLogoUrl(request.getLogoUrl());
        if (request.getDepartments() != null) entity.setDepartments(request.getDepartments());
        if (request.getSettings() != null) entity.setSettings(request.getSettings());
        entity.setUpdatedAt(new Date());

        return toDTO(entityRepository.save(entity));
    }

    @Override
    public List<EntityProfileDTO> searchEntities(String name, String city) {
        List<HealthcareEntity> results;
        if (name != null && city != null) {
            results = entityRepository.findByNameContainingIgnoreCaseAndStatus(name, EntityStatus.ACTIVE)
                    .stream().filter(e -> city.equalsIgnoreCase(e.getCity())).collect(Collectors.toList());
        } else if (name != null) {
            results = entityRepository.findByNameContainingIgnoreCaseAndStatus(name, EntityStatus.ACTIVE);
        } else if (city != null) {
            results = entityRepository.findByCityIgnoreCaseAndStatus(city, EntityStatus.ACTIVE);
        } else {
            results = entityRepository.findByStatus(EntityStatus.ACTIVE);
        }
        return results.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public EntityProfileDTO addMember(String entityId, AddMemberRequest request, String actorUserId) {
        HealthcareEntity entity = findOrThrow(entityId);

        boolean alreadyMember = entity.getMembers() != null && entity.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(request.getUserId()) && m.isActive());
        if (alreadyMember) {
            throw new IllegalStateException("User is already an active member of this entity");
        }

        EntityMember member = EntityMember.builder()
                .userId(request.getUserId())
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .role(request.getRole())
                .permissions(request.getPermissions())
                .departmentIds(request.getDepartmentIds())
                .joinedAt(new Date())
                .active(true)
                .build();

        if (entity.getMembers() == null) entity.setMembers(new ArrayList<>());
        entity.getMembers().add(member);
        entity.setUpdatedAt(new Date());

        return toDTO(entityRepository.save(entity));
    }

    @Override
    public EntityProfileDTO updateMemberRole(String entityId, String userId, EntityMemberRole newRole, String actorUserId) {
        HealthcareEntity entity = findOrThrow(entityId);

        entity.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId) && m.isActive())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("EntityMember", userId))
                .setRole(newRole);

        entity.setUpdatedAt(new Date());
        return toDTO(entityRepository.save(entity));
    }

    @Override
    public EntityProfileDTO removeMember(String entityId, String userId, String actorUserId) {
        HealthcareEntity entity = findOrThrow(entityId);

        entity.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId) && m.isActive())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("EntityMember", userId))
                .setActive(false);

        entity.setUpdatedAt(new Date());
        return toDTO(entityRepository.save(entity));
    }

    @Override
    public List<EntityProfileDTO> getEntityMembers(String entityId) {
        HealthcareEntity entity = findOrThrow(entityId);
        EntityProfileDTO dto = toDTO(entity);
        return List.of(dto);
    }

    private HealthcareEntity findOrThrow(String entityId) {
        return entityRepository.findByEntityId(entityId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareEntity", entityId));
    }

    private EntityProfileDTO toDTO(HealthcareEntity entity) {
        return modelMapper.map(entity, EntityProfileDTO.class);
    }
}

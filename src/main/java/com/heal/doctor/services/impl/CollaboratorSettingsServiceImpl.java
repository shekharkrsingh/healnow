package com.heal.doctor.services.impl;

import com.heal.doctor.dto.CollaboratorSettingsDTO;
import com.heal.doctor.models.CollaboratorSettingsEntity;
import com.heal.doctor.repositories.CollaboratorSettingsRepository;
import com.heal.doctor.services.ICollaboratorSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CollaboratorSettingsServiceImpl implements ICollaboratorSettingsService {

    private final CollaboratorSettingsRepository repository;

    @Override
    public CollaboratorSettingsDTO getSettingsByCollaboratorId(String collaboratorId) {
        Optional<CollaboratorSettingsEntity> optionalSettings = repository.findByCollaboratorId(collaboratorId);
        
        if (optionalSettings.isEmpty()) {
            return CollaboratorSettingsDTO.builder()
                    .collaboratorId(collaboratorId)
                    .enableNotifications(true) // default values
                    .twoFactorAuthEnabled(false) // default values
                    .build(); 
        }
        
        CollaboratorSettingsEntity entity = optionalSettings.get();
        return CollaboratorSettingsDTO.builder()
                .collaboratorId(entity.getCollaboratorId())
                .enableNotifications(entity.isEnableNotifications())
                .twoFactorAuthEnabled(entity.isTwoFactorAuthEnabled())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public CollaboratorSettingsDTO updateSettings(String collaboratorId, CollaboratorSettingsDTO settingsDTO) {
        CollaboratorSettingsEntity entity = repository.findByCollaboratorId(collaboratorId)
                .orElse(CollaboratorSettingsEntity.builder().collaboratorId(collaboratorId).build());
        
        entity.setEnableNotifications(settingsDTO.isEnableNotifications());
        entity.setTwoFactorAuthEnabled(settingsDTO.isTwoFactorAuthEnabled());
        entity.setUpdatedAt(new Date());
        
        repository.save(entity);
        
        settingsDTO.setCollaboratorId(collaboratorId);
        settingsDTO.setUpdatedAt(entity.getUpdatedAt());
        return settingsDTO;
    }
}

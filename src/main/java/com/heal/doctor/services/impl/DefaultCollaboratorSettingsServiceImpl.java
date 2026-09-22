package com.heal.doctor.services.impl;

import com.heal.doctor.dto.DefaultCollaboratorSettingsDTO;
import com.heal.doctor.models.DefaultCollaboratorSettingsEntity;
import com.heal.doctor.repositories.DefaultCollaboratorSettingsRepository;
import com.heal.doctor.services.IDefaultCollaboratorSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultCollaboratorSettingsServiceImpl implements IDefaultCollaboratorSettingsService {

    private final DefaultCollaboratorSettingsRepository repository;

    @Override
    public DefaultCollaboratorSettingsDTO getSettings() {
        Optional<DefaultCollaboratorSettingsEntity> optionalSettings = repository.findById(DefaultCollaboratorSettingsEntity.SINGLETON_ID);
        
        if (optionalSettings.isEmpty()) {
            return DefaultCollaboratorSettingsDTO.builder()
                    .enableNotifications(true)
                    .twoFactorAuthEnabled(false)
                    .build();
        }
        
        DefaultCollaboratorSettingsEntity entity = optionalSettings.get();
        return DefaultCollaboratorSettingsDTO.builder()
                .enableNotifications(entity.isEnableNotifications())
                .twoFactorAuthEnabled(entity.isTwoFactorAuthEnabled())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public DefaultCollaboratorSettingsDTO updateSettings(DefaultCollaboratorSettingsDTO settingsDTO) {
        DefaultCollaboratorSettingsEntity entity = repository.findById(DefaultCollaboratorSettingsEntity.SINGLETON_ID)
                .orElse(DefaultCollaboratorSettingsEntity.builder().id(DefaultCollaboratorSettingsEntity.SINGLETON_ID).build());
        
        entity.setEnableNotifications(settingsDTO.isEnableNotifications());
        entity.setTwoFactorAuthEnabled(settingsDTO.isTwoFactorAuthEnabled());
        entity.setUpdatedAt(new Date());
        
        repository.save(entity);
        
        settingsDTO.setUpdatedAt(entity.getUpdatedAt());
        return settingsDTO;
    }
}

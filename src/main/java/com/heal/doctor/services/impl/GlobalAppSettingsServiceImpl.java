package com.heal.doctor.services.impl;

import com.heal.doctor.dto.GlobalAppSettingsDTO;
import com.heal.doctor.models.GlobalAppSettingsEntity;
import com.heal.doctor.repositories.GlobalAppSettingsRepository;
import com.heal.doctor.services.IGlobalAppSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GlobalAppSettingsServiceImpl implements IGlobalAppSettingsService {

    private final GlobalAppSettingsRepository repository;

    @Override
    public GlobalAppSettingsDTO getSettings() {
        Optional<GlobalAppSettingsEntity> optionalSettings = repository.findById(GlobalAppSettingsEntity.SINGLETON_ID);
        
        if (optionalSettings.isEmpty()) {
            return new GlobalAppSettingsDTO(); // Default settings if none exist
        }
        
        GlobalAppSettingsEntity entity = optionalSettings.get();
        return GlobalAppSettingsDTO.builder()
                .maintenanceMode(entity.isMaintenanceMode())
                .allowNewRegistrations(entity.isAllowNewRegistrations())
                .contactEmail(entity.getContactEmail())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public GlobalAppSettingsDTO updateSettings(GlobalAppSettingsDTO settingsDTO) {
        GlobalAppSettingsEntity entity = repository.findById(GlobalAppSettingsEntity.SINGLETON_ID)
                .orElse(GlobalAppSettingsEntity.builder().id(GlobalAppSettingsEntity.SINGLETON_ID).build());
        
        entity.setMaintenanceMode(settingsDTO.isMaintenanceMode());
        entity.setAllowNewRegistrations(settingsDTO.isAllowNewRegistrations());
        entity.setContactEmail(settingsDTO.getContactEmail());
        entity.setUpdatedAt(new Date());
        
        repository.save(entity);
        
        settingsDTO.setUpdatedAt(entity.getUpdatedAt());
        return settingsDTO;
    }
}

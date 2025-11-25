package com.heal.doctor.services.impl;

import com.heal.doctor.dto.RuntimeApplicationConfigDTO;
import com.heal.doctor.models.RuntimeApplicationConfig;
import com.heal.doctor.repositories.RuntimeApplicationConfigRepository;
import com.heal.doctor.services.IRuntimeApplicationConfigService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import static com.heal.doctor.utils.SetIfNotEmpty.setIfNotEmpty;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class RuntimeApplicationConfigServices implements IRuntimeApplicationConfigService {

    private final RuntimeApplicationConfigRepository runtimeApplicationConfigRepository;
    private final ModelMapper modelMapper;

    @Override
    public RuntimeApplicationConfigDTO getRuntimeApplicationConfig() {
        RuntimeApplicationConfig config = runtimeApplicationConfigRepository
                .findById(RuntimeApplicationConfig.SINGLETON_ID)
                .orElseGet(() -> {
                    RuntimeApplicationConfig newConfig = RuntimeApplicationConfig.builder()
                            .id(RuntimeApplicationConfig.SINGLETON_ID)
                            .updatedAt(new Date())
                            .build();
                    return runtimeApplicationConfigRepository.save(newConfig);
                });

        return modelMapper.map(config, RuntimeApplicationConfigDTO.class);
    }

    @Override
    public RuntimeApplicationConfigDTO updateRuntimeApplicationConfig(RuntimeApplicationConfigDTO dto) {
        RuntimeApplicationConfig config = runtimeApplicationConfigRepository
                .findById(RuntimeApplicationConfig.SINGLETON_ID)
                .orElseGet(RuntimeApplicationConfig::new);


        setIfNotEmpty(dto.getMinVersion(), config::setMinVersion);
        setIfNotEmpty(dto.getLatestVersion(), config::setLatestVersion);
        setIfNotEmpty(dto.getAppWebUrl(), config::setAppWebUrl);
        setIfNotEmpty(dto.getGooglePlayStoreUrl(), config::setGooglePlayStoreUrl);
        setIfNotEmpty(dto.getApplePlayStoreUrl(), config::setApplePlayStoreUrl);
        setIfNotEmpty(dto.getLastUpdatedBy(), config::setLastUpdatedBy);
        setIfNotEmpty(dto.getLastUpdatedById(), config::setLastUpdatedById);
        setIfNotEmpty(dto.getAppName(), config::setAppName);
        setIfNotEmpty(dto.getAppSlogan(), config::setAppSlogan);
        setIfNotEmpty(dto.getSupportEmail(), config::setSupportEmail);
        config.setId(RuntimeApplicationConfig.SINGLETON_ID);
        config.setUpdatedAt(new Date());

        return modelMapper.map(runtimeApplicationConfigRepository.save(config), RuntimeApplicationConfigDTO.class);
    }

}

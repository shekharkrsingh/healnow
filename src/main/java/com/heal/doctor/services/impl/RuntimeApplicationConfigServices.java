package com.heal.doctor.services.impl;

import com.heal.doctor.dto.RuntimeApplicationConfigDTO;
import com.heal.doctor.models.RuntimeApplicationConfig;
import com.heal.doctor.repositories.RuntimeApplicationConfigRepository;
import com.heal.doctor.services.IRuntimeApplicationConfigService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

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
    public RuntimeApplicationConfigDTO updateRuntimeApplicationConfig(RuntimeApplicationConfigDTO runtimeApplication) {
        RuntimeApplicationConfig config = runtimeApplicationConfigRepository
                .findById(RuntimeApplicationConfig.SINGLETON_ID)
                .orElseGet(RuntimeApplicationConfig::new);

        modelMapper.map(runtimeApplication, config);

        config.setId(RuntimeApplicationConfig.SINGLETON_ID);
        config.setUpdatedAt(new Date());

        RuntimeApplicationConfig runtimeApplicationConfig= runtimeApplicationConfigRepository.save(config);

        return modelMapper.map(runtimeApplicationConfig, RuntimeApplicationConfigDTO.class);
    }
}

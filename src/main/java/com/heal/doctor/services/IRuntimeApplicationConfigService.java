package com.heal.doctor.services;

import com.heal.doctor.dto.RuntimeApplicationConfigDTO;

public interface IRuntimeApplicationConfigService {
    RuntimeApplicationConfigDTO getRuntimeApplicationConfig();
    RuntimeApplicationConfigDTO updateRuntimeApplicationConfig(RuntimeApplicationConfigDTO runtimeApplication);
}

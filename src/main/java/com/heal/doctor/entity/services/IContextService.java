package com.heal.doctor.entity.services;

import com.heal.doctor.entity.dto.ContextSwitchRequest;
import com.heal.doctor.entity.dto.UserContextOptionDTO;
import com.heal.doctor.entity.security.EffectiveContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface IContextService {

    EffectiveContext resolveContext(HttpServletRequest request);

    List<UserContextOptionDTO> getAvailableContexts(String userId);

    void setActiveContext(String userId, ContextSwitchRequest request);
}

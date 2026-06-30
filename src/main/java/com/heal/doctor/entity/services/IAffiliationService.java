package com.heal.doctor.entity.services;

import com.heal.doctor.entity.dto.AffiliationDetailDTO;
import com.heal.doctor.entity.dto.AffiliationListDTO;
import com.heal.doctor.entity.security.EffectiveContext;

import java.util.List;

public interface IAffiliationService {

    List<AffiliationListDTO> listAffiliations(EffectiveContext ctx);

    AffiliationDetailDTO getAffiliationDetail(String affiliationId, EffectiveContext ctx);
}

package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.dto.AffiliationDetailDTO;
import com.heal.doctor.entity.dto.AffiliationListDTO;
import com.heal.doctor.entity.models.EntityAffiliation;
import com.heal.doctor.entity.models.enums.AffiliationStatus;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.security.EffectiveContext;
import com.heal.doctor.entity.services.IAffiliationService;
import com.heal.doctor.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AffiliationServiceImpl implements IAffiliationService {

    private final EntityAffiliationRepository affiliationRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<AffiliationListDTO> listAffiliations(EffectiveContext ctx) {
        List<EntityAffiliation> affiliations;

        if (ctx.isEntityScoped()) {
            affiliations = affiliationRepository.findByEntityId(ctx.getEntityId());
        } else if (ctx.isDoctorScoped()) {
            affiliations = affiliationRepository.findByDoctorId(ctx.getDoctorId());
        } else if (ctx.isAffiliationScoped()) {
            affiliations = affiliationRepository.findByAffiliationId(ctx.getAffiliationId())
                    .map(List::of).orElse(List.of());
        } else {
            affiliations = List.of();
        }

        return affiliations.stream()
                .map(a -> modelMapper.map(a, AffiliationListDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public AffiliationDetailDTO getAffiliationDetail(String affiliationId, EffectiveContext ctx) {
        EntityAffiliation aff = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityAffiliation", affiliationId));

        validateAccess(aff, ctx);

        return modelMapper.map(aff, AffiliationDetailDTO.class);
    }

    private void validateAccess(EntityAffiliation aff, EffectiveContext ctx) {
        boolean hasAccess = (ctx.isDoctorScoped() && aff.getDoctorId().equals(ctx.getDoctorId()))
                || (ctx.isEntityScoped() && aff.getEntityId().equals(ctx.getEntityId()))
                || (ctx.isAffiliationScoped() && aff.getAffiliationId().equals(ctx.getAffiliationId()))
                || "ADMIN".equals(ctx.getPrimaryRole());

        if (!hasAccess) {
            throw new com.heal.doctor.exception.ForbiddenException("Access denied to affiliation: " + aff.getAffiliationId());
        }
    }
}

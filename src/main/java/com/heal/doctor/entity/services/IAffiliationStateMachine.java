package com.heal.doctor.entity.services;

import com.heal.doctor.entity.dto.AffiliationDetailDTO;
import com.heal.doctor.entity.dto.AffiliationInitiateRequest;

public interface IAffiliationStateMachine {

    AffiliationDetailDTO initiate(AffiliationInitiateRequest request, String actorUserId);

    AffiliationDetailDTO acceptPeer(String affiliationId, String actorUserId);

    AffiliationDetailDTO rejectPeer(String affiliationId, String actorUserId, String reason);

    AffiliationDetailDTO approveAdmin(String affiliationId, String adminUserId);

    AffiliationDetailDTO rejectAdmin(String affiliationId, String adminUserId, String reason);

    AffiliationDetailDTO suspend(String affiliationId, String actorUserId, String reason);

    AffiliationDetailDTO reinstate(String affiliationId, String adminUserId);

    AffiliationDetailDTO terminate(String affiliationId, String actorUserId, String reason);
}

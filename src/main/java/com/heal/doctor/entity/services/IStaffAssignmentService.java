package com.heal.doctor.entity.services;

import com.heal.doctor.entity.dto.AssignStaffRequest;
import com.heal.doctor.entity.dto.StaffAssignmentDTO;
import com.heal.doctor.entity.security.EffectiveContext;

import java.util.List;

public interface IStaffAssignmentService {

    StaffAssignmentDTO assignStaff(String affiliationId, AssignStaffRequest request, EffectiveContext ctx);

    List<StaffAssignmentDTO> listStaff(String affiliationId, EffectiveContext ctx);

    void revokeStaff(String affiliationId, String userId, EffectiveContext ctx);

    void revokeAllForAffiliation(String affiliationId);
}

package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.EntityMember;
import com.heal.doctor.entity.models.EntitySettings;
import com.heal.doctor.entity.models.enums.EntityStatus;
import com.heal.doctor.entity.models.enums.EntityType;
import com.heal.doctor.models.enums.VerificationStatus;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class EntityProfileDTO {

    private String entityId;
    private String name;
    private EntityType type;
    private EntityStatus status;
    private VerificationStatus verificationStatus;

    private String registrationNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phoneNumber;
    private String email;
    private String logoUrl;

    private List<String> departments;
    private List<EntityMember> members;
    private EntitySettings settings;

    private String createdByUserId;
    private Date createdAt;
    private Date updatedAt;
}

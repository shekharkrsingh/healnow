package com.heal.doctor.entity.dto;

import com.heal.doctor.entity.models.EntitySettings;
import com.heal.doctor.entity.models.enums.EntityType;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateEntityRequest {

    @Size(min = 2, max = 200)
    private String name;

    private EntityType type;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 10)
    private String pincode;

    @Size(max = 15)
    private String phoneNumber;

    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String logoUrl;

    private List<String> departments;

    private EntitySettings settings;
}

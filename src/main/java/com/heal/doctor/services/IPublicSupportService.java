package com.heal.doctor.services;

import com.heal.doctor.dto.PublicContactRequestDTO;

public interface IPublicSupportService {
    void handlePublicInquiry(PublicContactRequestDTO requestDTO);
}

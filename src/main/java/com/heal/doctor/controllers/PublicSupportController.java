package com.heal.doctor.controllers;

import com.heal.doctor.dto.PublicContactRequestDTO;
import com.heal.doctor.services.IPublicSupportService;
import com.heal.doctor.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/support")
@RequiredArgsConstructor
public class PublicSupportController {

    private final IPublicSupportService publicSupportService;

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Void>> handlePublicContact(@Valid @RequestBody PublicContactRequestDTO requestDTO) {
        publicSupportService.handlePublicInquiry(requestDTO);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Your inquiry has been submitted successfully. We will get back to you soon.")
                .build());
    }
}

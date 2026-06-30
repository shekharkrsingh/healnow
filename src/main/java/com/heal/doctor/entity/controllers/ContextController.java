package com.heal.doctor.entity.controllers;

import com.heal.doctor.entity.dto.ContextSwitchRequest;
import com.heal.doctor.entity.dto.UserContextOptionDTO;
import com.heal.doctor.entity.services.IContextService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contexts")
@RequiredArgsConstructor
public class ContextController {

    private final IContextService contextService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserContextOptionDTO>>> getAvailableContexts() {
        String userId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contextService.getAvailableContexts(userId)));
    }

    @PutMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> setActiveContext(@RequestBody ContextSwitchRequest request) {
        String userId = CurrentUserName.getCurrentUserId();
        contextService.setActiveContext(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Active context updated", null));
    }
}

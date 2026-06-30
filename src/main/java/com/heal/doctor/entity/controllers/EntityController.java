package com.heal.doctor.entity.controllers;

import com.heal.doctor.entity.dto.AddMemberRequest;
import com.heal.doctor.entity.dto.CreateEntityRequest;
import com.heal.doctor.entity.dto.EntityProfileDTO;
import com.heal.doctor.entity.dto.UpdateEntityRequest;
import com.heal.doctor.entity.models.enums.EntityMemberRole;
import com.heal.doctor.entity.services.IEntityService;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.utils.CurrentUserName;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
public class EntityController {

    private final IEntityService entityService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EntityProfileDTO>> createEntity(@Valid @RequestBody CreateEntityRequest request) {
        String userId = CurrentUserName.getCurrentUserId();
        EntityProfileDTO dto = entityService.createEntity(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Entity created successfully", dto));
    }

    @GetMapping("/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EntityProfileDTO>> getEntity(@PathVariable String entityId) {
        return ResponseEntity.ok(ApiResponse.success(entityService.getEntityById(entityId)));
    }

    @PutMapping("/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EntityProfileDTO>> updateEntity(
            @PathVariable String entityId,
            @Valid @RequestBody UpdateEntityRequest request) {
        String userId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Entity updated", entityService.updateEntity(entityId, request, userId)));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EntityProfileDTO>>> searchEntities(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(ApiResponse.success(entityService.searchEntities(name, city)));
    }

    @GetMapping("/{entityId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EntityProfileDTO>>> getMembers(@PathVariable String entityId) {
        return ResponseEntity.ok(ApiResponse.success(entityService.getEntityMembers(entityId)));
    }

    @PostMapping("/{entityId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EntityProfileDTO>> addMember(
            @PathVariable String entityId,
            @Valid @RequestBody AddMemberRequest request) {
        String userId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Member added", entityService.addMember(entityId, request, userId)));
    }

    @PutMapping("/{entityId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EntityProfileDTO>> updateMemberRole(
            @PathVariable String entityId,
            @PathVariable String userId,
            @RequestParam EntityMemberRole role) {
        String actorId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Member role updated", entityService.updateMemberRole(entityId, userId, role, actorId)));
    }

    @DeleteMapping("/{entityId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EntityProfileDTO>> removeMember(
            @PathVariable String entityId,
            @PathVariable String userId) {
        String actorId = CurrentUserName.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Member removed", entityService.removeMember(entityId, userId, actorId)));
    }
}

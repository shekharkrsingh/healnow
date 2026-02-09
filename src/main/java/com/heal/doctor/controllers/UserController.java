package com.heal.doctor.controllers;


import com.heal.doctor.dto.ChangePasswordDTO;
import com.heal.doctor.dto.UpdateEmailDTO;
import com.heal.doctor.services.IUserService;
import com.heal.doctor.services.impl.RuntimeApplicationConfigServices;
import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.dto.*;
import com.heal.doctor.dto.RogerProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'COLLABORATOR', 'ROGER')")
public class UserController {

    private final IUserService userService;


    @PostMapping("/update-email")
    public ResponseEntity<ApiResponse<String>> updateEmail(@RequestBody UpdateEmailDTO updateEmailDTO) {
        String newAuthToken=userService.updateEmail(updateEmailDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Email updated successfully", newAuthToken));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(changePasswordDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
    }

    @PutMapping(value = "/profile/picture", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> changeProfilePicture(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = userService.changeProfilePicture(file);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Profile picture updated successfully", imageUrl)
        );
    }

    @PutMapping(value = "/cover/picture", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> changeCoverPicture(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = userService.changeCoverPicture(file);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cover picture updated successfully", imageUrl)
        );
    }

}

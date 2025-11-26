package com.heal.doctor.controllers;


import com.heal.doctor.utils.ApiResponse;
import com.heal.doctor.dto.*;
import com.heal.doctor.services.IDoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/doctors")

public class DoctorController {


    private final IDoctorService doctorService;




    @PutMapping()
    public ResponseEntity<ApiResponse<DoctorDTO>> updateDoctor(@Valid @RequestBody UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
        DoctorDTO savedDoctorDTO = doctorService.updateDoctor(updateDoctorDetailsDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor updated successfully", savedDoctorDTO));
    }



    @PostMapping("/update-email")
    public ResponseEntity<ApiResponse<String>> updateEmail(@RequestBody UpdateEmailDTO updateEmailDTO) {
        String newAuthToken=doctorService.updateEmail(updateEmailDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Email updated successfully", newAuthToken));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        doctorService.changePassword(changePasswordDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorDTO>> getDoctorProfile() {
        DoctorDTO doctorDTO = doctorService.getDoctorProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", doctorDTO));
    }

    @PostMapping(value = "/changeProfilePicture", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> changeProfilePicture(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "file is required", null));
        }

        String url = doctorService.changeProfilePicture(file);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Profile Picture updated successfully",
                        url
                )
        );
    }


    @PostMapping(value = "/changeCoverPicture", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> changeCoverPicture(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "file is required", null));
        }

        String url = doctorService.changeCoverPicture(file);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Cover Picture updated successfully",
                        url
                )
        );
    }

}

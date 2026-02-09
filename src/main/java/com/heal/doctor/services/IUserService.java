package com.heal.doctor.services;

import com.heal.doctor.dto.ChangePasswordDTO;
import com.heal.doctor.dto.ForgotPasswordDTO;
import com.heal.doctor.dto.UpdateEmailDTO;
import com.heal.doctor.dto.RogerProfileDTO;
import com.heal.doctor.dto.RogerRegistrationDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService {
    void forgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    String updateEmail( UpdateEmailDTO updateEmailDTO);
    void changePassword(ChangePasswordDTO changePasswordDTO);
    String login(String username, String password);
    String changeProfilePicture(MultipartFile file);
    String changeCoverPicture(MultipartFile file);
}

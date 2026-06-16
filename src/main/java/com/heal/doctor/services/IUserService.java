package com.heal.doctor.services;

import com.heal.doctor.dto.ChangePasswordDTO;
import com.heal.doctor.dto.ForgotPasswordDTO;
import com.heal.doctor.dto.LoginResponseDTO;
import com.heal.doctor.dto.UpdateEmailDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService {
    void forgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    LoginResponseDTO updateEmail(UpdateEmailDTO updateEmailDTO);
    void changePassword(ChangePasswordDTO changePasswordDTO);
    LoginResponseDTO login(String username, String password);
    String changeProfilePicture(MultipartFile file);
    String changeCoverPicture(MultipartFile file);
    LoginResponseDTO refreshAccessToken(String rawRefreshToken);
    void revokeRefreshToken(String rawRefreshToken);
    void revokeAllUserTokens(String userId);
}

package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AcceptAdminInviteDTO;
import com.heal.doctor.dto.AdminInviteRequestDTO;
import com.heal.doctor.models.AdminInvitationEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.InvitationStatus;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.AdminInvitationRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.IAdminInvitationService;
import com.heal.doctor.services.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInvitationServiceImpl implements IAdminInvitationService {

    private final AdminInvitationRepository adminInvitationRepository;
    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public AdminInvitationEntity sendInvitation(AdminInviteRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Generate token and ID
        String token = UUID.randomUUID().toString();
        String invitationId = "ADMIN-INV-" + UUID.randomUUID().toString().substring(0, 8);
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000)); // 7 days

        AdminInvitationEntity invitation = AdminInvitationEntity.builder()
                .invitationId(invitationId)
                .invitationToken(token)
                .email(requestDTO.getEmail())
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .status(InvitationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();

        AdminInvitationEntity saved = adminInvitationRepository.save(invitation);

        // Send email
        sendAdminInviteEmail(saved);

        return saved;
    }

    @Override
    public void acceptInvitation(AcceptAdminInviteDTO acceptDTO) {
        AdminInvitationEntity invitation = adminInvitationRepository.findByInvitationToken(acceptDTO.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is no longer valid");
        }

        if (invitation.getExpiresAt().before(new Date())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            adminInvitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitation has expired");
        }

        // Create User
        UserEntity adminUser = UserEntity.builder()
                .userId("ADMIN-" + UUID.randomUUID().toString().substring(0, 8))
                .email(invitation.getEmail())
                .password(passwordEncoder.encode(acceptDTO.getPassword()))
                .rolesEnum(RolesEnum.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        userRepository.save(adminUser);

        // Update invitation
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(new Date());
        invitation.setUpdatedAt(new Date());
        adminInvitationRepository.save(invitation);
    }

    @Override
    public List<AdminInvitationEntity> getAllPendingInvitations() {
        return adminInvitationRepository.findAll();
    }

    private void sendAdminInviteEmail(AdminInvitationEntity invitation) {
        try {
            String inviteLink = frontendUrl + "/admin/accept-invite/" + invitation.getInvitationToken();
            Map<String, Object> variables = new HashMap<>();
            variables.put("firstName", invitation.getFirstName());
            variables.put("inviteLink", inviteLink);
            variables.put("companyName", "Heal Now");

            emailService.sendHtmlEmail(
                    invitation.getEmail(),
                    "You have been invited as an Administrator",
                    "admin-invitation.template.html",
                    variables
            );
        } catch (Exception e) {
            log.error("Failed to send admin invite email to {}", invitation.getEmail(), e);
        }
    }
}

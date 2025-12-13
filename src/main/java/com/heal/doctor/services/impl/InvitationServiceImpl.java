package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AcceptInvitationDTO;
import com.heal.doctor.dto.InvitationRequestDTO;
import com.heal.doctor.dto.InvitationResponseDTO;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ConflictException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.exception.ValidationException;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.InvitationEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.InvitationStatus;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.InvitationRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.IEmailService;
import com.heal.doctor.services.IInvitationService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements IInvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationServiceImpl.class);
    private static final String INVITATION_ID_PREFIX = "INV-";
    private static final String USER_ID_PREFIX = "USR-";
    private static final String COLLABORATOR_ID_PREFIX = "COL-";
    private static final String INVITATION_ID_DATE_FORMAT = "yyyyMMdd";
    private static final int INVITATION_EXPIRY_HOURS = 72; // 3 days
    private static final int TOKEN_LENGTH = 32;

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final DoctorRepository doctorRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Value("${company.name:HealNow}")
    private String companyName;

    @Value("${frontend.domain:http://localhost:8081}")
    private String websiteUrl;

    @Override
    @Transactional
    public InvitationResponseDTO sendInvitation(String doctorId, InvitationRequestDTO requestDTO) {
        logger.info("Sending invitation: doctorId: {}, email: {}", doctorId, requestDTO.getEmail());

        // Check if email already exists in UserRepository
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            logger.warn("Invitation failed - email already exists: {}", requestDTO.getEmail());
            throw new ConflictException("User", "A user with this email already exists");
        }

        // Check if there's already a pending invitation for this email
        List<InvitationEntity> existingInvitations = invitationRepository.findByEmailAndStatus(
                requestDTO.getEmail(), InvitationStatus.PENDING);
        if (!existingInvitations.isEmpty()) {
            logger.warn("Invitation failed - pending invitation already exists: {}", requestDTO.getEmail());
            throw new ConflictException("Invitation", "A pending invitation for this email already exists");
        }

        // Generate unique invitation token
        String invitationToken = generateInvitationToken();

        // Generate invitation ID
        String invitationId = generateInvitationId();

        // Calculate expiration date
        Date expiresAt = new Date(System.currentTimeMillis() + (INVITATION_EXPIRY_HOURS * 60 * 60 * 1000L));

        // Create invitation entity
        InvitationEntity invitation = InvitationEntity.builder()
                .invitationId(invitationId)
                .invitationToken(invitationToken)
                .doctorId(doctorId)
                .email(requestDTO.getEmail())
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .status(InvitationStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        InvitationEntity savedInvitation = invitationRepository.save(invitation);
        logger.info("Invitation created: invitationId: {}, email: {}", invitationId, requestDTO.getEmail());

        // Get doctor name for email
        String doctorName = "Doctor";
        try {
            DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId).orElse(null);
            if (doctor != null) {
                doctorName = doctor.getFirstName() + " " + doctor.getLastName();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch doctor name for invitation email: {}", e.getMessage());
        }

        // Send invitation email
        String invitationLink = websiteUrl + "/accept-invitation?token=" + invitationToken;
        emailService.sendHtmlEmail(
                requestDTO.getEmail(),
                "Invitation to Collaborate - " + companyName,
                "collaborator-invitation.template",
                Map.of(
                        "firstName", requestDTO.getFirstName(),
                        "doctorName", doctorName,
                        "invitationLink", invitationLink,
                        "expiresIn", INVITATION_EXPIRY_HOURS + " hours",
                        "companyName", companyName
                )
        ).exceptionally(ex -> {
            logger.error("Failed to send invitation email: email: {}, error: {}", requestDTO.getEmail(), ex.getMessage(), ex);
            return null;
        });

        return modelMapper.map(savedInvitation, InvitationResponseDTO.class);
    }

    @Override
    @Transactional
    public InvitationResponseDTO acceptInvitation(AcceptInvitationDTO acceptDTO) {
        logger.info("Accepting invitation: token: {}", acceptDTO.getToken());

        // Find invitation by token
        InvitationEntity invitation = invitationRepository.findByInvitationToken(acceptDTO.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", "token"));

        // Validate invitation
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            logger.warn("Invitation already processed: invitationId: {}, status: {}", 
                    invitation.getInvitationId(), invitation.getStatus());
            throw new BadRequestException("Invitation has already been processed");
        }

        if (invitation.getExpiresAt().before(new Date())) {
            logger.warn("Invitation expired: invitationId: {}, expiresAt: {}", 
                    invitation.getInvitationId(), invitation.getExpiresAt());
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException("Invitation has expired");
        }

        // Check if email already has UserEntity
        if (userRepository.existsByEmail(invitation.getEmail())) {
            logger.warn("User already exists: email: {}", invitation.getEmail());
            throw new ConflictException("User", "A user with this email already exists");
        }

        // Generate user ID
        String userId = generateUserId();

        // Create UserEntity
        UserEntity user = UserEntity.builder()
                .userId(userId)
                .email(invitation.getEmail())
                .password(passwordEncoder.encode(acceptDTO.getPassword()))
                .rolesEnum(RolesEnum.COLLABORATOR)
                .isActive(true)
                .emailVerified(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        UserEntity savedUser = userRepository.save(user);
        logger.info("User created: userId: {}, email: {}", userId, invitation.getEmail());

        // Generate collaborator ID (same as userId)
        String collaboratorId = userId;

        // Create CollaboratorProfileEntity
        CollaboratorProfileEntity collaboratorProfile = CollaboratorProfileEntity.builder()
                .collaboratorId(collaboratorId)
                .doctorId(invitation.getDoctorId())
                .firstName(invitation.getFirstName())
                .lastName(invitation.getLastName())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        collaboratorProfileRepository.save(collaboratorProfile);
        logger.info("Collaborator profile created: collaboratorId: {}, doctorId: {}", 
                collaboratorId, invitation.getDoctorId());

        // Update invitation status
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(new Date());
        invitation.setUpdatedAt(new Date());
        invitationRepository.save(invitation);

        return modelMapper.map(invitation, InvitationResponseDTO.class);
    }

    @Override
    public List<InvitationResponseDTO> getInvitationsByDoctor(String doctorId) {
        logger.debug("Fetching invitations for doctor: {}", doctorId);
        List<InvitationEntity> invitations = invitationRepository.findByDoctorId(doctorId);
        return invitations.stream()
                .map(invitation -> modelMapper.map(invitation, InvitationResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeInvitation(String invitationId) {
        logger.info("Revoking invitation: invitationId: {}", invitationId);
        InvitationEntity invitation = invitationRepository.findByInvitationId(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Only pending invitations can be revoked");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setUpdatedAt(new Date());
        invitationRepository.save(invitation);
        logger.info("Invitation revoked: invitationId: {}", invitationId);
    }

    @Override
    public boolean validateInvitationToken(String token) {
        logger.debug("Validating invitation token: {}", token);
        InvitationEntity invitation = invitationRepository.findByInvitationToken(token)
                .orElse(null);

        if (invitation == null) {
            return false;
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            return false;
        }

        if (invitation.getExpiresAt().before(new Date())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            return false;
        }

        return true;
    }

    private String generateInvitationToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateInvitationId() {
        String timestamp = new SimpleDateFormat(INVITATION_ID_DATE_FORMAT).format(new Date());
        String randomSuffix = String.format("%04d", new SecureRandom().nextInt(10000));
        return INVITATION_ID_PREFIX + timestamp + "-" + randomSuffix;
    }

    private String generateUserId() {
        String timestamp = new SimpleDateFormat(INVITATION_ID_DATE_FORMAT).format(new Date());
        String randomSuffix = String.format("%06d", new SecureRandom().nextInt(1000000));
        return USER_ID_PREFIX + timestamp + "-" + randomSuffix;
    }
}

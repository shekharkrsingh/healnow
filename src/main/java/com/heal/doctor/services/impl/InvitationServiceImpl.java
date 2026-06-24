package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AcceptInvitationDTO;
import com.heal.doctor.dto.InvitationRequestDTO;
import com.heal.doctor.dto.InvitationResponseDTO;
import com.heal.doctor.models.enums.CollaboratorStatus;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ConflictException;
import com.heal.doctor.exception.ForbiddenException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.exception.ValidationException;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.DoctorAssociation;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

        userRepository.findByEmail(requestDTO.getEmail()).ifPresent(user -> {
            CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(user.getUserId()).orElse(null);
            if (user.getRolesEnum() != RolesEnum.COLLABORATOR || (profile != null
                    && !(profile.getStatus() == CollaboratorStatus.REMOVED
                    || profile.getStatus() == CollaboratorStatus.INVITED)
            ) ) {
                logger.warn("Invitation failed - email already exists and is active or not a collaborator: {}", requestDTO.getEmail());
                throw new ConflictException("User", "A user with this email already exists and is either active or not a collaborator");
            }
            logger.info("Allowing invitation for removed collaborator: {}", requestDTO.getEmail());
        });

        List<InvitationEntity> existingInvitations = invitationRepository.findByEmailAndDoctorId(requestDTO.getEmail(), doctorId);
        
        // Check for existing valid pending invitations to RESEND
        for (InvitationEntity inv : existingInvitations) {
            // Case 1: Valid Pending Invitation -> Resend Email
            if (inv.getStatus() == InvitationStatus.PENDING && inv.getExpiresAt().after(new Date())) {
                 logger.info("Resending existing pending invitation to: {}", requestDTO.getEmail());
                 
                 // Update names in case of typo correction
                 inv.setFirstName(requestDTO.getFirstName());
                 inv.setLastName(requestDTO.getLastName());
                 inv.setUpdatedAt(new Date());
                 invitationRepository.save(inv);
                 
                 sendCollaborationInvitationEmail(inv);
                 
                 return modelMapper.map(inv, InvitationResponseDTO.class);
            }
            
            // Case 2: Expired (or pending but past expiry) -> Revoke so we can create a fresh one
            if (inv.getStatus() == InvitationStatus.EXPIRED || (inv.getStatus() == InvitationStatus.PENDING && inv.getExpiresAt().before(new Date()))) {
                inv.setStatus(InvitationStatus.REVOKED);
                inv.setUpdatedAt(new Date());
                invitationRepository.save(inv);
            }
        }

        // Revoke all other pending invitations for this email from other doctors
        List<InvitationEntity> otherPendingInvitations = invitationRepository.findByEmailAndStatus(requestDTO.getEmail(), InvitationStatus.PENDING);
        for (InvitationEntity inv : otherPendingInvitations) {
            if (!inv.getDoctorId().equals(doctorId)) {
                logger.info("Revoking prior pending invitation from doctor {} for email {}", inv.getDoctorId(), requestDTO.getEmail());
                inv.setStatus(InvitationStatus.REVOKED);
                inv.setUpdatedAt(new Date());
                invitationRepository.save(inv);
            }
        }

        String invitationToken = COLLABORATOR_ID_PREFIX + generateInvitationToken();
        String collaboratorId;
        UserEntity existingUser = userRepository.findByEmail(requestDTO.getEmail()).orElse(null);
        if (existingUser != null) {
            collaboratorId = existingUser.getUserId();
        } else {
            collaboratorId = generateCollaboratorId();
        }

        InvitationEntity invitation = InvitationEntity.builder()
                .invitationId(generateInvitationId())
                .invitationToken(invitationToken)
                .doctorId(doctorId)
                .email(requestDTO.getEmail())
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .collaboratorId(collaboratorId)
                .status(InvitationStatus.PENDING)
                .expiresAt(new Date(System.currentTimeMillis() + (INVITATION_EXPIRY_HOURS * 3600000L)))
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        invitationRepository.save(invitation);

        // Create/Update CollaboratorProfile with INVITED status
        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(collaboratorId).orElse(null);
        if (profile == null) {
            profile = CollaboratorProfileEntity.builder()
                    .collaboratorId(collaboratorId)
                    .doctorId(doctorId)
                    .firstName(requestDTO.getFirstName())
                    .lastName(requestDTO.getLastName())
                    .email(requestDTO.getEmail())
                    .status(CollaboratorStatus.INVITED)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
        } else {
            profile.setDoctorId(doctorId);
            profile.setStatus(CollaboratorStatus.INVITED);
            profile.setUpdatedAt(new Date());
            profile.setEmail(requestDTO.getEmail());
        }
        collaboratorProfileRepository.save(profile);
        logger.info("Invitation created: invitationId: {}, email: {}", invitation.getInvitationId(), requestDTO.getEmail());

        sendCollaborationInvitationEmail(invitation);

        return modelMapper.map(invitation, InvitationResponseDTO.class);
    }

    private void sendCollaborationInvitationEmail(InvitationEntity invitation) {
        // Get doctor name for email
        String doctorName = "Doctor";
        try {
            DoctorEntity doctor = doctorRepository.findByDoctorId(invitation.getDoctorId()).orElse(null);
            if (doctor != null) {
                doctorName = doctor.getFirstName() + " " + doctor.getLastName();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch doctor name for invitation email: {}", e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        // Send invitation email
        String invitationLink = websiteUrl + "/accept-invitation?token=" + invitation.getInvitationToken();
        emailService.sendHtmlEmail(
                invitation.getEmail(),
                "Invitation to Collaborate - " + companyName,
                "collaborator-invitation.template.html",
                Map.of(
                        "firstName", invitation.getFirstName(),
                        "doctorName", doctorName,
                        "invitationLink", invitationLink,
                        "expiresIn", INVITATION_EXPIRY_HOURS + " hours",
                        "companyName", companyName,
                        "invitationDate", formattedDate
                )
        ).exceptionally(ex -> {
            logger.error("Failed to send invitation email: email: {}, error: {}", invitation.getEmail(), ex.getMessage(), ex);
            return null;
        });
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

        // Use collaboratorId from invitation
        String userId = invitation.getCollaboratorId();
        UserEntity user = userRepository.findByUserId(userId).orElse(null);
        
        if (user == null) {
            // New user flow
            user = UserEntity.builder()
                    .userId(userId)
                    .email(invitation.getEmail())
                    .password(passwordEncoder.encode(acceptDTO.getPassword()))
                    .rolesEnum(RolesEnum.COLLABORATOR)
                    .isActive(true)
                    .emailVerified(false)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
            logger.info("Creating new user for invitation: userId: {}", userId);
        } else {
            // Re-invitation flow: update existing user
            user.setPassword(passwordEncoder.encode(acceptDTO.getPassword()));
            user.setIsActive(true);
            user.setUpdatedAt(new Date());
            logger.info("Updating existing user for re-invitation: userId: {}", userId);
        }

        UserEntity savedUser = userRepository.save(user);

        // Generate collaborator ID (same as userId)
        String collaboratorId = userId;

        // Build doctor association with cached doctor info
        String invitedDoctorId = invitation.getDoctorId();
        DoctorEntity doctor = doctorRepository.findByDoctorId(invitedDoctorId).orElse(null);
        String doctorName = doctor != null
                ? "Dr. " + doctor.getFirstName() + " " + doctor.getLastName()
                : invitedDoctorId;
        String specialization = doctor != null ? doctor.getSpecialization() : null;
        String clinicName = doctor != null ? doctor.getClinicName() : null;

        DoctorAssociation newAssociation = DoctorAssociation.builder()
                .doctorId(invitedDoctorId)
                .doctorName(doctorName)
                .specialization(specialization)
                .clinicName(clinicName)
                .role(null) // Role can be set per-doctor later
                .joinedAt(new Date())
                .active(true)
                .build();

        // Check if profile exists
        CollaboratorProfileEntity collaboratorProfile = collaboratorProfileRepository.findByCollaboratorId(collaboratorId).orElse(null);
        
        if (collaboratorProfile == null) {
            // Create New Profile with first association
            List<DoctorAssociation> associations = new ArrayList<>();
            associations.add(newAssociation);

            collaboratorProfile = CollaboratorProfileEntity.builder()
                    .collaboratorId(collaboratorId)
                    .doctorId(invitedDoctorId) // Legacy field for backward compat
                    .doctorAssociations(associations)
                    .activeDoctorId(invitedDoctorId)
                    .firstName(invitation.getFirstName())
                    .lastName(invitation.getLastName())
                    .email(invitation.getEmail())
                    .status(CollaboratorStatus.ACTIVATED)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
            logger.info("Creating new collaborator profile: collaboratorId: {}, doctorId: {}", collaboratorId, invitedDoctorId);
        } else {
            // Existing profile — append new doctor association
            List<DoctorAssociation> associations = collaboratorProfile.getDoctorAssociations();
            if (associations == null) {
                associations = new ArrayList<>();
            }

            // Check for duplicate association
            boolean alreadyAssociated = associations.stream()
                    .anyMatch(a -> a.getDoctorId().equals(invitedDoctorId) && a.isActive());
            if (alreadyAssociated) {
                throw new ConflictException("DoctorAssociation", "Collaborator is already associated with this doctor");
            }

            // Re-activate if previously deactivated
            boolean reactivated = false;
            for (DoctorAssociation a : associations) {
                if (a.getDoctorId().equals(invitedDoctorId) && !a.isActive()) {
                    a.setActive(true);
                    a.setJoinedAt(new Date());
                    a.setDoctorName(doctorName);
                    a.setSpecialization(specialization);
                    a.setClinicName(clinicName);
                    reactivated = true;
                    break;
                }
            }
            if (!reactivated) {
                associations.add(newAssociation);
            }

            collaboratorProfile.setDoctorAssociations(associations);
            collaboratorProfile.setFirstName(invitation.getFirstName());
            collaboratorProfile.setLastName(invitation.getLastName());
            collaboratorProfile.setEmail(invitation.getEmail());
            collaboratorProfile.setStatus(CollaboratorStatus.ACTIVATED);
            collaboratorProfile.setUpdatedAt(new Date());
            logger.info("Appended doctor association for collaborator: collaboratorId: {}, doctorId: {}", collaboratorId, invitedDoctorId);
        }

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
        
        // Lazy expiration check: update status if expired
        Date now = new Date();
        invitations.forEach(invitation -> {
            if (invitation.getStatus() == InvitationStatus.PENDING && invitation.getExpiresAt().before(now)) {
                invitation.setStatus(InvitationStatus.EXPIRED);
                invitation.setUpdatedAt(now);
                invitationRepository.save(invitation);
            }
        });
        
        return invitations.stream()
                .filter(invitation -> {
                    if (invitation.getStatus() == InvitationStatus.REVOKED) {
                        return false;
                    }
                    if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
                        // Strict check: Only show accepted invitation if the collaborator is currently ACTIVATED or DEACTIVATED
                        // If they are REMOVED or have been Re-INVITED (new cycle), hide this old accepted invitation.
                        return collaboratorProfileRepository.findByCollaboratorId(invitation.getCollaboratorId())
                                .map(profile -> profile.getStatus() == CollaboratorStatus.ACTIVATED || profile.getStatus() == CollaboratorStatus.DEACTIVATED)
                                .orElse(false); // If profile missing (orphan), hide it
                    }
                    return true;
                })
                .map(invitation -> modelMapper.map(invitation, InvitationResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeInvitation(String invitationId, String doctorId) {
        logger.info("Revoking invitation: invitationId: {} by doctorId: {}", invitationId, doctorId);
        InvitationEntity invitation = invitationRepository.findByInvitationId(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));

        if (!doctorId.equals(invitation.getDoctorId())) {
            throw new ForbiddenException("invitation", "revoke");
        }

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

    private String generateCollaboratorId() {
        String timestamp = new SimpleDateFormat(INVITATION_ID_DATE_FORMAT).format(new Date());
        String randomSuffix = String.format("%06d", new SecureRandom().nextInt(1000000));
        return COLLABORATOR_ID_PREFIX + timestamp + "-" + randomSuffix;
    }
}

package com.heal.doctor.services.impl;

import com.heal.doctor.Mail.ICollaboratorMailService;
import com.heal.doctor.dto.CollaboratorDTO;
import com.heal.doctor.dto.UpdateCollaboratorProfileDTO;
import com.heal.doctor.dto.UserDTO;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.CollaboratorStatus;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.ICollaboratorService;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollaboratorServiceImpl implements ICollaboratorService {

    private static final Logger logger = LoggerFactory.getLogger(CollaboratorServiceImpl.class);

    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final ICollaboratorMailService collaboratorMailService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public List<CollaboratorDTO> getCollaboratorsByDoctor(String doctorId) {
        logger.debug("Fetching collaborators for doctor: {}", doctorId);
        List<CollaboratorProfileEntity> profiles = collaboratorProfileRepository.findByDoctorId(doctorId);
        
        return profiles.stream()
                .map(profile -> modelMapper.map(profile, CollaboratorDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateCollaborator(String collaboratorId) {
        logger.info("Deactivating collaborator: {}", collaboratorId);
        UserEntity user = userRepository.findByUserId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator user", collaboratorId));

        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator profile", collaboratorId));

        if (!user.getIsActive() && profile.getStatus() == CollaboratorStatus.DEACTIVATED) {
            throw new BadRequestException("Collaborator is already deactivated");
        }

        user.setIsActive(false);
        userRepository.save(user);

        profile.setStatus(CollaboratorStatus.DEACTIVATED);
        profile.setUpdatedAt(new Date());
        collaboratorProfileRepository.save(profile);

        logger.info("Collaborator deactivated: {}", collaboratorId);
        
        sendStatusChangeEmails(profile, user);
    }

    @Override
    @Transactional
    public void activateCollaborator(String collaboratorId) {
        logger.info("Activating collaborator: {}", collaboratorId);
        UserEntity user = userRepository.findByUserId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator user", collaboratorId));

        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator profile", collaboratorId));

        if (user.getIsActive() && profile.getStatus() == CollaboratorStatus.ACTIVATED) {
            throw new BadRequestException("Collaborator is already active");
        }

        user.setIsActive(true);
        userRepository.save(user);

        profile.setStatus(CollaboratorStatus.ACTIVATED);
        profile.setUpdatedAt(new Date());
        collaboratorProfileRepository.save(profile);

        logger.info("Collaborator activated: {}", collaboratorId);
        
        sendStatusChangeEmails(profile, user);
    }

    @Override
    @Transactional
    public void removeCollaborator(String collaboratorId) {
        logger.info("Removing collaborator from doctor: {}", collaboratorId);
        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator profile", collaboratorId));

        // Save info for email before clearing
        String doctorId = profile.getDoctorId();
        String colleagueEmail = profile.getEmail(); // Use email from profile

        // Update profile: clear doctor link and set status
        profile.setDoctorId(null);
        profile.setStatus(CollaboratorStatus.REMOVED);
        profile.setUpdatedAt(new Date());
        collaboratorProfileRepository.save(profile);

        // Update user: deactivate and reset password to random (if user exists)
        userRepository.findByUserId(collaboratorId).ifPresent(user -> {
            user.setIsActive(false);
            String randomPassword = UUID.randomUUID().toString();
            user.setPassword(passwordEncoder.encode(randomPassword));
            user.setUpdatedAt(new Date());
            userRepository.save(user);
            logger.info("Associated user deactivated and password reset: {}", collaboratorId);
        });

        logger.info("Collaborator removed (released): {}", collaboratorId);

        // Send emails
        if (doctorId != null && colleagueEmail != null) {
            DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId).orElse(null);
            if (doctor != null) {
                UserEntity doctorUser = userRepository.findByUserId(doctorId).orElse(null);
                if (doctorUser != null) {
                    collaboratorMailService.sendRemovalEmail(
                            doctor.getFirstName() + " " + doctor.getLastName(),
                            doctorUser.getEmail(),
                            profile.getFirstName() + " " + profile.getLastName(),
                            colleagueEmail
                    );
                }
            }
        }
    }

    private void sendStatusChangeEmails(CollaboratorProfileEntity profile, UserEntity user) {
        if (profile.getDoctorId() != null) {
            DoctorEntity doctor = doctorRepository.findByDoctorId(profile.getDoctorId()).orElse(null);
            if (doctor != null) {
                UserEntity doctorUser = userRepository.findByUserId(profile.getDoctorId()).orElse(null);
                if (doctorUser != null) {
                    String docName = doctor.getFirstName() + " " + doctor.getLastName();
                    String colName = profile.getFirstName() + " " + profile.getLastName();
                    
                    if (profile.getStatus() == CollaboratorStatus.ACTIVATED) {
                        collaboratorMailService.sendActivationEmail(docName, doctorUser.getEmail(), colName, user.getEmail());
                    } else if (profile.getStatus() == CollaboratorStatus.DEACTIVATED) {
                        collaboratorMailService.sendDeactivationEmail(docName, doctorUser.getEmail(), colName, user.getEmail());
                    }
                }
            }
        }
    }

    @Override
    public UserDTO getCollaboratorProfile() {
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        String userId = CurrentUserName.getCurrentUserId();
        
        logger.debug("Fetching collaborator profile: email: {}, userId: {}, doctorId: {}", username, userId, doctorId);
        
        CollaboratorProfileEntity collaboratorProfile = collaboratorProfileRepository.findByCollaboratorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator profile", userId));
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        
        // Map collaborator profile to userDTO (for compatibility with frontend)
        UserDTO userDTO = UserDTO.builder()
                .firstName(collaboratorProfile.getFirstName())
                .lastName(collaboratorProfile.getLastName())
                .doctorId(doctorId) // Associated doctor's ID
                .email(user.getEmail())
                .build();
        
        logger.debug("Collaborator profile retrieved: collaboratorId: {}, doctorId: {}", 
                collaboratorProfile.getCollaboratorId(), doctorId);
        return userDTO;
    }

    @Override
    @Transactional
    public UserDTO updateCollaboratorProfile(UpdateCollaboratorProfileDTO updateDTO) {
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        String userId = CurrentUserName.getCurrentUserId();
        
        logger.info("Updating collaborator profile: email: {}, userId: {}, doctorId: {}", username, userId, doctorId);
        
        CollaboratorProfileEntity collaboratorProfile = collaboratorProfileRepository.findByCollaboratorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator profile", userId));
        
        // Only allow firstName and lastName updates
        if (updateDTO.getFirstName() != null && !updateDTO.getFirstName().isEmpty()) {
            collaboratorProfile.setFirstName(updateDTO.getFirstName());
        }
        if (updateDTO.getLastName() != null && !updateDTO.getLastName().isEmpty()) {
            collaboratorProfile.setLastName(updateDTO.getLastName());
        }
        
        collaboratorProfile.setUpdatedAt(new Date());
        CollaboratorProfileEntity updatedProfile = collaboratorProfileRepository.save(collaboratorProfile);
        
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        
        UserDTO userDTO = UserDTO.builder()
                .firstName(updatedProfile.getFirstName())
                .lastName(updatedProfile.getLastName())
                .doctorId(doctorId) // Associated doctor's ID
                .email(user.getEmail())
                .build();
        
        logger.info("Collaborator profile updated: collaboratorId: {}, doctorId: {}", userId, doctorId);
        return userDTO;
    }
}

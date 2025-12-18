package com.heal.doctor.services.impl;

import com.heal.doctor.dto.CollaboratorDTO;
import com.heal.doctor.dto.UserDTO;
import com.heal.doctor.dto.UpdateCollaboratorProfileDTO;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.ICollaboratorService;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollaboratorServiceImpl implements ICollaboratorService {

    private static final Logger logger = LoggerFactory.getLogger(CollaboratorServiceImpl.class);

    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CollaboratorDTO> getCollaboratorsByDoctor(String doctorId) {
        logger.debug("Fetching collaborators for doctor: {}", doctorId);
        List<CollaboratorProfileEntity> profiles = collaboratorProfileRepository.findByDoctorId(doctorId);
        
        return profiles.stream()
                .map(profile -> {
                    UserEntity user = userRepository.findByUserId(profile.getCollaboratorId())
                            .orElse(null);
                    if (user == null) {
                        logger.warn("User not found for collaboratorId: {}", profile.getCollaboratorId());
                        return null;
                    }
                    CollaboratorDTO dto = modelMapper.map(profile, CollaboratorDTO.class);
                    dto.setEmail(user.getEmail());
                    dto.setIsActive(user.getIsActive());
                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateCollaborator(String collaboratorId) {
        logger.info("Deactivating collaborator: {}", collaboratorId);
        UserEntity user = userRepository.findByUserId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator", collaboratorId));

        if (!user.getIsActive()) {
            throw new BadRequestException("Collaborator is already deactivated");
        }

        user.setIsActive(false);
        userRepository.save(user);
        logger.info("Collaborator deactivated: {}", collaboratorId);
    }

    @Override
    @Transactional
    public void activateCollaborator(String collaboratorId) {
        logger.info("Activating collaborator: {}", collaboratorId);
        UserEntity user = userRepository.findByUserId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator", collaboratorId));

        if (user.getIsActive()) {
            throw new BadRequestException("Collaborator is already active");
        }

        user.setIsActive(true);
        userRepository.save(user);
        logger.info("Collaborator activated: {}", collaboratorId);
    }

    @Override
    @Transactional
    public void removeCollaborator(String collaboratorId) {
        logger.info("Removing collaborator: {}", collaboratorId);
        CollaboratorProfileEntity profile = collaboratorProfileRepository.findByCollaboratorId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator", collaboratorId));

        // Deactivate the user instead of deleting (soft delete)
        UserEntity user = userRepository.findByUserId(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", collaboratorId));

        user.setIsActive(false);
        userRepository.save(user);

        // Optionally remove the profile association
        // collaboratorProfileRepository.delete(profile);
        
        logger.info("Collaborator removed (deactivated): {}", collaboratorId);
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

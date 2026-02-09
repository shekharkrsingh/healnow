package com.heal.doctor.security;

import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.RogerEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.RogerRepository;
import com.heal.doctor.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorUserDetailsService.class);

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final RogerRepository rogerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);
        
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        RolesEnum role = user.getRolesEnum();
        if (role == null) {
            // Default to DOCTOR for backward compatibility
            role = RolesEnum.DOCTOR;
        }

        switch (role) {
            case DOCTOR:
                DoctorEntity doctorProfile = doctorRepository.findByDoctorId(user.getUserId())
                        .orElseThrow(() -> new UsernameNotFoundException("Doctor profile not found for userId: " + user.getUserId()));
                logger.debug("Loaded doctor user: userId={}, doctorId={}", user.getUserId(), doctorProfile.getDoctorId());
                return new DoctorUserDetails(user, doctorProfile);

            case COLLABORATOR:
                CollaboratorProfileEntity collaboratorProfile = collaboratorProfileRepository.findByCollaboratorId(user.getUserId())
                        .orElseThrow(() -> new UsernameNotFoundException("Collaborator profile not found for userId: " + user.getUserId()));
                logger.debug("Loaded collaborator user: userId={}, doctorId={}", user.getUserId(), collaboratorProfile.getDoctorId());
                return new CollaboratorUserDetails(user, collaboratorProfile);

            case ROGER:
                RogerEntity roger = rogerRepository.findByRogerId(user.getUserId())
                        .orElseThrow(() -> new UsernameNotFoundException("Roger profile not found for userId: " + user.getUserId()));
                logger.debug("Loaded Roger user: userId={}, rogerId={}", user.getUserId(), roger.getRogerId());
                DoctorEntity rogerProfilePlaceholder = DoctorEntity.builder()
                        .doctorId(roger.getRogerId())
                        .firstName(roger.getFirstName())
                        .lastName(roger.getLastName())
                        .phoneNumber(roger.getPhoneNumber())
                        .build();
                return new DoctorUserDetails(user, rogerProfilePlaceholder);

            case ADMIN:
            case USER:
            default:
                // For ADMIN and USER, create a minimal doctor profile with just the doctorId
                // This allows them to use the same UserDetails structure
                DoctorEntity adminProfile = DoctorEntity.builder()
                        .doctorId(user.getUserId())
                        .build();
                logger.debug("Loaded user with role: {}", role);
                return new DoctorUserDetails(user, adminProfile);
        }
    }
}

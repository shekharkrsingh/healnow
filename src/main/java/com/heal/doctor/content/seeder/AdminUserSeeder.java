package com.heal.doctor.content.seeder;

import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for default admin user...");

        String adminEmail = "admin@healnow.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("Default admin user not found. Seeding default admin user...");
            
            UserEntity admin = UserEntity.builder()
                    .userId("ADMIN-001")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("AdminPassword123!"))
                    .rolesEnum(RolesEnum.ADMIN)
                    .isActive(true)
                    .emailVerified(true)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();

            userRepository.save(admin);
            log.info("Default admin user seeded successfully with email: {} and userId: ADMIN-001", adminEmail);
        } else {
            log.info("Default admin user already exists.");
        }
    }
}

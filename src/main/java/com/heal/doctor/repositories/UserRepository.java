package com.heal.doctor.repositories;

import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.RolesEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUserId(String userId);
    List<UserEntity> findByRolesEnum(RolesEnum rolesEnum);
    boolean existsByEmail(String email);
}

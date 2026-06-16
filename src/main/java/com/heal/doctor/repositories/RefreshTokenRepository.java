package com.heal.doctor.repositories;

import com.heal.doctor.models.RefreshTokenEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshTokenEntity, String> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findByTokenFamily(String tokenFamily);

    void deleteAllByUserId(String userId);

    void deleteByTokenHash(String tokenHash);
}

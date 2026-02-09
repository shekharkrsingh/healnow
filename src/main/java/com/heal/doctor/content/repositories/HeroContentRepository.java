package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.HeroContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HeroContentRepository extends MongoRepository<HeroContent, String> {
    Optional<HeroContent> findFirstByIsActiveTrue();
}

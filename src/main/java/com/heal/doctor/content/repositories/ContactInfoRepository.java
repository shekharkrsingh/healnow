package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.ContactInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactInfoRepository extends MongoRepository<ContactInfo, String> {
    Optional<ContactInfo> findFirstByIsActiveTrue();
}

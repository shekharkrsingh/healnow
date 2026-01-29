package com.heal.doctor.repositories;

import com.heal.doctor.models.PublicInquiryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicInquiryRepository extends MongoRepository<PublicInquiryEntity, String> {
}

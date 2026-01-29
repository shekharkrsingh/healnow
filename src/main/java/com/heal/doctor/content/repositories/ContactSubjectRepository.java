package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.ContactSubject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactSubjectRepository extends MongoRepository<ContactSubject, String> {
    List<ContactSubject> findByIsActiveTrueOrderByDisplayOrder();
}

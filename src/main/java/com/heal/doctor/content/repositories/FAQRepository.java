package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.FAQ;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FAQRepository extends MongoRepository<FAQ, String> {
    List<FAQ> findByIsActiveTrueOrderByDisplayOrder();
    List<FAQ> findByIsActiveTrueAndCategoryOrderByDisplayOrder(String category);
}

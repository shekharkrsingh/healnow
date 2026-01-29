package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.Testimonial;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestimonialRepository extends MongoRepository<Testimonial, String> {
    List<Testimonial> findByIsActiveTrueOrderByDisplayOrder();
    List<Testimonial> findByIsActiveTrueAndTypeOrderByDisplayOrder(String type);
}

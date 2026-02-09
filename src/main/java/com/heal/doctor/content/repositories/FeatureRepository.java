package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.Feature;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureRepository extends MongoRepository<Feature, String> {
    List<Feature> findByIsActiveTrueOrderByDisplayOrder();
}

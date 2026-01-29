package com.heal.doctor.content.repositories;

import com.heal.doctor.content.models.OnboardingStep;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OnboardingStepRepository extends MongoRepository<OnboardingStep, String> {
    List<OnboardingStep> findByIsActiveTrueOrderByStepNumber();
}

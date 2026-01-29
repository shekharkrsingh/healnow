package com.heal.doctor.content.services;

import com.heal.doctor.content.dto.*;

import java.util.List;

public interface IContentService {
    
    SiteStatisticsDTO getStatistics();
    void updateStatistics(SiteStatisticsDTO dto);
    
    HeroContentDTO getHeroContent();
    void updateHeroContent(HeroContentDTO dto);
    
    ContactInfoDTO getContactInfo();
    void updateContactInfo(ContactInfoDTO dto);
    
    List<ContactSubjectDTO> getActiveContactSubjects();
    List<ContactSubjectDTO> getAllContactSubjects();
    ContactSubjectDTO createContactSubject(ContactSubjectDTO dto);
    ContactSubjectDTO updateContactSubject(String id, ContactSubjectDTO dto);
    void deleteContactSubject(String id);
    
    List<FAQDTO> getActiveFAQs(String category);
    List<FAQDTO> getAllFAQs();
    FAQDTO createFAQ(FAQDTO dto);
    FAQDTO updateFAQ(String id, FAQDTO dto);
    void deleteFAQ(String id);
    
    List<TestimonialDTO> getActiveTestimonials(String type);
    List<TestimonialDTO> getAllTestimonials();
    TestimonialDTO createTestimonial(TestimonialDTO dto);
    TestimonialDTO updateTestimonial(String id, TestimonialDTO dto);
    void deleteTestimonial(String id);
    
    List<FeatureDTO> getActiveFeatures();
    List<FeatureDTO> getAllFeatures();
    FeatureDTO createFeature(FeatureDTO dto);
    FeatureDTO updateFeature(String id, FeatureDTO dto);
    void deleteFeature(String id);
    
    List<ValuePropositionDTO> getActiveValuePropositions();
    List<ValuePropositionDTO> getAllValuePropositions();
    ValuePropositionDTO createValueProposition(ValuePropositionDTO dto);
    ValuePropositionDTO updateValueProposition(String id, ValuePropositionDTO dto);
    void deleteValueProposition(String id);
    
    List<OnboardingStepDTO> getActiveOnboardingSteps();
    List<OnboardingStepDTO> getAllOnboardingSteps();
    OnboardingStepDTO createOnboardingStep(OnboardingStepDTO dto);
    OnboardingStepDTO updateOnboardingStep(String id, OnboardingStepDTO dto);
    void deleteOnboardingStep(String id);
    
    SiteSettingsDTO getSiteSettings();
    void updateSiteSettings(SiteSettingsDTO dto);
}

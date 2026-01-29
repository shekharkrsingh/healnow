package com.heal.doctor.content.services.impl;

import com.heal.doctor.content.dto.*;
import com.heal.doctor.content.models.*;
import com.heal.doctor.content.repositories.*;
import com.heal.doctor.content.services.IContentService;
import com.heal.doctor.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements IContentService {

    private final SiteStatisticsRepository statisticsRepository;
    private final HeroContentRepository heroContentRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final ContactSubjectRepository contactSubjectRepository;
    private final FAQRepository faqRepository;
    private final TestimonialRepository testimonialRepository;
    private final FeatureRepository featureRepository;
    private final ValuePropositionRepository valuePropositionRepository;
    private final OnboardingStepRepository onboardingStepRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final ModelMapper modelMapper;

    @Override
    public SiteStatisticsDTO getStatistics() {
        SiteStatistics entity = statisticsRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("SiteStatistics", "active=true"));
        return modelMapper.map(entity, SiteStatisticsDTO.class);
    }

    @Override
    public void updateStatistics(SiteStatisticsDTO dto) {
        SiteStatistics entity = statisticsRepository.findFirstByIsActiveTrue()
                .orElse(new SiteStatistics());
        modelMapper.map(dto, entity);
        entity.setUpdatedAt(Instant.now());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        statisticsRepository.save(entity);
    }

    @Override
    public HeroContentDTO getHeroContent() {
        HeroContent entity = heroContentRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("HeroContent", "active=true"));
        return modelMapper.map(entity, HeroContentDTO.class);
    }

    @Override
    public void updateHeroContent(HeroContentDTO dto) {
        HeroContent entity = heroContentRepository.findFirstByIsActiveTrue()
                .orElse(new HeroContent());
        modelMapper.map(dto, entity);
        entity.setUpdatedAt(Instant.now());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        heroContentRepository.save(entity);
    }

    @Override
    public ContactInfoDTO getContactInfo() {
        ContactInfo entity = contactInfoRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("ContactInfo", "active=true"));
        return modelMapper.map(entity, ContactInfoDTO.class);
    }

    @Override
    public void updateContactInfo(ContactInfoDTO dto) {
        ContactInfo entity = contactInfoRepository.findFirstByIsActiveTrue()
                .orElse(new ContactInfo());
        modelMapper.map(dto, entity);
        entity.setUpdatedAt(Instant.now());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        contactInfoRepository.save(entity);
    }

    @Override
    public List<ContactSubjectDTO> getActiveContactSubjects() {
        return contactSubjectRepository.findByIsActiveTrueOrderByDisplayOrder().stream()
                .map(e -> modelMapper.map(e, ContactSubjectDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ContactSubjectDTO> getAllContactSubjects() {
        return contactSubjectRepository.findAll().stream()
                .map(e -> modelMapper.map(e, ContactSubjectDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public ContactSubjectDTO createContactSubject(ContactSubjectDTO dto) {
        ContactSubject entity = modelMapper.map(dto, ContactSubject.class);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        ContactSubject saved = contactSubjectRepository.save(entity);
        return modelMapper.map(saved, ContactSubjectDTO.class);
    }

    @Override
    public ContactSubjectDTO updateContactSubject(String id, ContactSubjectDTO dto) {
        ContactSubject entity = contactSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactSubject", id));
        modelMapper.map(dto, entity);
        entity.setId(id);
        entity.setUpdatedAt(Instant.now());
        ContactSubject saved = contactSubjectRepository.save(entity);
        return modelMapper.map(saved, ContactSubjectDTO.class);
    }

    @Override
    public void deleteContactSubject(String id) {
        contactSubjectRepository.deleteById(id);
    }

    @Override
    public List<FAQDTO> getActiveFAQs(String category) {
        List<FAQ> faqs = category != null && !category.isEmpty()
                ? faqRepository.findByIsActiveTrueAndCategoryOrderByDisplayOrder(category)
                : faqRepository.findByIsActiveTrueOrderByDisplayOrder();
        return faqs.stream()
                .map(e -> modelMapper.map(e, FAQDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<FAQDTO> getAllFAQs() {
        return faqRepository.findAll().stream()
                .map(e -> modelMapper.map(e, FAQDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public FAQDTO createFAQ(FAQDTO dto) {
        FAQ entity = modelMapper.map(dto, FAQ.class);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        FAQ saved = faqRepository.save(entity);
        return modelMapper.map(saved, FAQDTO.class);
    }

    @Override
    public FAQDTO updateFAQ(String id, FAQDTO dto) {
        FAQ entity = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ", id));
        modelMapper.map(dto, entity);
        entity.setId(id);
        entity.setUpdatedAt(Instant.now());
        FAQ saved = faqRepository.save(entity);
        return modelMapper.map(saved, FAQDTO.class);
    }

    @Override
    public void deleteFAQ(String id) {
        faqRepository.deleteById(id);
    }

    @Override
    public List<TestimonialDTO> getActiveTestimonials(String type) {
        List<Testimonial> testimonials = type != null && !type.isEmpty()
                ? testimonialRepository.findByIsActiveTrueAndTypeOrderByDisplayOrder(type)
                : testimonialRepository.findByIsActiveTrueOrderByDisplayOrder();
        return testimonials.stream()
                .map(e -> modelMapper.map(e, TestimonialDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestimonialDTO> getAllTestimonials() {
        return testimonialRepository.findAll().stream()
                .map(e -> modelMapper.map(e, TestimonialDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public TestimonialDTO createTestimonial(TestimonialDTO dto) {
        Testimonial entity = modelMapper.map(dto, Testimonial.class);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        Testimonial saved = testimonialRepository.save(entity);
        return modelMapper.map(saved, TestimonialDTO.class);
    }

    @Override
    public TestimonialDTO updateTestimonial(String id, TestimonialDTO dto) {
        Testimonial entity = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", id));
        modelMapper.map(dto, entity);
        entity.setId(id);
        entity.setUpdatedAt(Instant.now());
        Testimonial saved = testimonialRepository.save(entity);
        return modelMapper.map(saved, TestimonialDTO.class);
    }

    @Override
    public void deleteTestimonial(String id) {
        testimonialRepository.deleteById(id);
    }

    @Override
    public List<FeatureDTO> getActiveFeatures() {
        return featureRepository.findByIsActiveTrueOrderByDisplayOrder().stream()
                .map(e -> modelMapper.map(e, FeatureDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<FeatureDTO> getAllFeatures() {
        return featureRepository.findAll().stream()
                .map(e -> modelMapper.map(e, FeatureDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public FeatureDTO createFeature(FeatureDTO dto) {
        Feature entity = modelMapper.map(dto, Feature.class);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        Feature saved = featureRepository.save(entity);
        return modelMapper.map(saved, FeatureDTO.class);
    }

    @Override
    public FeatureDTO updateFeature(String id, FeatureDTO dto) {
        Feature entity = featureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature", id));
        modelMapper.map(dto, entity);
        entity.setId(id);
        entity.setUpdatedAt(Instant.now());
        Feature saved = featureRepository.save(entity);
        return modelMapper.map(saved, FeatureDTO.class);
    }

    @Override
    public void deleteFeature(String id) {
        featureRepository.deleteById(id);
    }

    @Override
    public List<ValuePropositionDTO> getActiveValuePropositions() {
        return valuePropositionRepository.findByIsActiveTrueOrderByDisplayOrder().stream()
                .map(e -> modelMapper.map(e, ValuePropositionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ValuePropositionDTO> getAllValuePropositions() {
        return valuePropositionRepository.findAll().stream()
                .map(e -> modelMapper.map(e, ValuePropositionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public ValuePropositionDTO createValueProposition(ValuePropositionDTO dto) {
        ValueProposition entity = modelMapper.map(dto, ValueProposition.class);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        ValueProposition saved = valuePropositionRepository.save(entity);
        return modelMapper.map(saved, ValuePropositionDTO.class);
    }

    @Override
    public ValuePropositionDTO updateValueProposition(String id, ValuePropositionDTO dto) {
        ValueProposition entity = valuePropositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ValueProposition", id));
        modelMapper.map(dto, entity);
        entity.setId(id);
        entity.setUpdatedAt(Instant.now());
        ValueProposition saved = valuePropositionRepository.save(entity);
        return modelMapper.map(saved, ValuePropositionDTO.class);
    }

    @Override
    public void deleteValueProposition(String id) {
        valuePropositionRepository.deleteById(id);
    }

    @Override
    public List<OnboardingStepDTO> getActiveOnboardingSteps() {
        return onboardingStepRepository.findByIsActiveTrueOrderByStepNumber().stream()
                .map(e -> modelMapper.map(e, OnboardingStepDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<OnboardingStepDTO> getAllOnboardingSteps() {
        return onboardingStepRepository.findAll().stream()
                .map(e -> modelMapper.map(e, OnboardingStepDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public OnboardingStepDTO createOnboardingStep(OnboardingStepDTO dto) {
        OnboardingStep entity = modelMapper.map(dto, OnboardingStep.class);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        OnboardingStep saved = onboardingStepRepository.save(entity);
        return modelMapper.map(saved, OnboardingStepDTO.class);
    }

    @Override
    public OnboardingStepDTO updateOnboardingStep(String id, OnboardingStepDTO dto) {
        OnboardingStep entity = onboardingStepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingStep", id));
        modelMapper.map(dto, entity);
        entity.setId(id);
        entity.setUpdatedAt(Instant.now());
        OnboardingStep saved = onboardingStepRepository.save(entity);
        return modelMapper.map(saved, OnboardingStepDTO.class);
    }

    @Override
    public void deleteOnboardingStep(String id) {
        onboardingStepRepository.deleteById(id);
    }

    @Override
    public SiteSettingsDTO getSiteSettings() {
        SiteSettings entity = siteSettingsRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("SiteSettings", "active=true"));
        return modelMapper.map(entity, SiteSettingsDTO.class);
    }

    @Override
    public void updateSiteSettings(SiteSettingsDTO dto) {
        SiteSettings entity = siteSettingsRepository.findFirstByIsActiveTrue()
                .orElse(new SiteSettings());
        modelMapper.map(dto, entity);
        entity.setUpdatedAt(Instant.now());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        siteSettingsRepository.save(entity);
    }
}

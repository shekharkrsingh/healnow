package com.heal.doctor.content.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heal.doctor.content.models.*;
import com.heal.doctor.content.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentDataSeeder implements CommandLineRunner {

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
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting content data seeding...");

        seedSiteStatistics();
        seedHeroContent();
        seedContactInfo();
        seedContactSubjects();
        seedFAQs();
        seedTestimonials();
        seedFeatures();
        seedValuePropositions();
        seedOnboardingSteps();
        seedSiteSettings();

        log.info("Content data seeding completed successfully!");
    }

    private void seedSiteStatistics() throws Exception {
        if (statisticsRepository.count() == 0) {
            SiteStatistics stats = objectMapper.readValue(
                    new ClassPathResource("seed-data/site-statistics.json").getInputStream(),
                    SiteStatistics.class
            );
            stats.setCreatedAt(Instant.now());
            stats.setUpdatedAt(Instant.now());
            statisticsRepository.save(stats);
            log.info("Seeded new site statistics");
        }
    }

    private void seedHeroContent() throws Exception {
        if (heroContentRepository.count() == 0) {
            HeroContent hero = objectMapper.readValue(
                    new ClassPathResource("seed-data/hero-content.json").getInputStream(),
                    HeroContent.class
            );
            hero.setCreatedAt(Instant.now());
            hero.setUpdatedAt(Instant.now());
            heroContentRepository.save(hero);
            log.info("Seeded hero content");
        }
    }

    private void seedContactInfo() throws Exception {
        if (contactInfoRepository.count() == 0) {
            ContactInfo contact = objectMapper.readValue(
                    new ClassPathResource("seed-data/contact-info.json").getInputStream(),
                    ContactInfo.class
            );
            contact.setCreatedAt(Instant.now());
            contact.setUpdatedAt(Instant.now());
            contactInfoRepository.save(contact);
            log.info("Seeded contact info");
        }
    }

    private void seedContactSubjects() throws Exception {
        if (contactSubjectRepository.count() == 0) {
            List<ContactSubject> subjects = objectMapper.readValue(
                    new ClassPathResource("seed-data/contact-subjects.json").getInputStream(),
                    new TypeReference<List<ContactSubject>>() {}
            );
            subjects.forEach(s -> {
                s.setCreatedAt(Instant.now());
                s.setUpdatedAt(Instant.now());
            });
            contactSubjectRepository.saveAll(subjects);
            log.info("Seeded {} contact subjects", subjects.size());
        }
    }

    private void seedFAQs() throws Exception {
        if (faqRepository.count() == 0) {
            List<FAQ> faqs = objectMapper.readValue(
                    new ClassPathResource("seed-data/faqs.json").getInputStream(),
                    new TypeReference<List<FAQ>>() {}
            );
            faqs.forEach(f -> {
                f.setCreatedAt(Instant.now());
                f.setUpdatedAt(Instant.now());
            });
            faqRepository.saveAll(faqs);
            log.info("Seeded {} FAQs", faqs.size());
        }
    }

    private void seedTestimonials() throws Exception {
        if (testimonialRepository.count() == 0) {
            List<Testimonial> testimonials = objectMapper.readValue(
                    new ClassPathResource("seed-data/testimonials.json").getInputStream(),
                    new TypeReference<List<Testimonial>>() {}
            );
            testimonials.forEach(t -> {
                t.setCreatedAt(Instant.now());
                t.setUpdatedAt(Instant.now());
            });
            testimonialRepository.saveAll(testimonials);
            log.info("Seeded {} testimonials", testimonials.size());
        }
    }

    private void seedFeatures() throws Exception {
        if (featureRepository.count() == 0) {
            List<Feature> features = objectMapper.readValue(
                    new ClassPathResource("seed-data/features.json").getInputStream(),
                    new TypeReference<List<Feature>>() {}
            );
            features.forEach(f -> {
                f.setCreatedAt(Instant.now());
                f.setUpdatedAt(Instant.now());
            });
            featureRepository.saveAll(features);
            log.info("Seeded {} features", features.size());
        }
    }

    private void seedValuePropositions() throws Exception {
        if (valuePropositionRepository.count() == 0) {
            List<ValueProposition> props = objectMapper.readValue(
                    new ClassPathResource("seed-data/value-propositions.json").getInputStream(),
                    new TypeReference<List<ValueProposition>>() {}
            );
            props.forEach(p -> {
                p.setCreatedAt(Instant.now());
                p.setUpdatedAt(Instant.now());
            });
            valuePropositionRepository.saveAll(props);
            log.info("Seeded {} value propositions", props.size());
        }
    }

    private void seedOnboardingSteps() throws Exception {
        if (onboardingStepRepository.count() == 0) {
            List<OnboardingStep> steps = objectMapper.readValue(
                    new ClassPathResource("seed-data/onboarding-steps.json").getInputStream(),
                    new TypeReference<List<OnboardingStep>>() {}
            );
            steps.forEach(s -> {
                s.setCreatedAt(Instant.now());
                s.setUpdatedAt(Instant.now());
            });
            onboardingStepRepository.saveAll(steps);
            log.info("Seeded {} onboarding steps", steps.size());
        }
    }

    private void seedSiteSettings() throws Exception {
        if (siteSettingsRepository.count() == 0) {
            SiteSettings settings = objectMapper.readValue(
                    new ClassPathResource("seed-data/site-settings.json").getInputStream(),
                    SiteSettings.class
            );
            settings.setCreatedAt(Instant.now());
            settings.setUpdatedAt(Instant.now());
            siteSettingsRepository.save(settings);
            log.info("Seeded new site settings");
        }
    }
}

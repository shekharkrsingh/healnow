package com.heal.doctor.content.controllers;

import com.heal.doctor.content.dto.*;
import com.heal.doctor.content.services.IContentService;
import com.heal.doctor.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/content")
@RequiredArgsConstructor
public class PublicContentController {

    private final IContentService contentService;

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<SiteStatisticsDTO>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.<SiteStatisticsDTO>builder()
                .success(true)
                .data(contentService.getStatistics())
                .build());
    }

    @GetMapping("/hero")
    public ResponseEntity<ApiResponse<HeroContentDTO>> getHeroContent() {
        return ResponseEntity.ok(ApiResponse.<HeroContentDTO>builder()
                .success(true)
                .data(contentService.getHeroContent())
                .build());
    }

    @GetMapping("/contact-info")
    public ResponseEntity<ApiResponse<ContactInfoDTO>> getContactInfo() {
        return ResponseEntity.ok(ApiResponse.<ContactInfoDTO>builder()
                .success(true)
                .data(contentService.getContactInfo())
                .build());
    }

    @GetMapping("/contact-subjects")
    public ResponseEntity<ApiResponse<List<ContactSubjectDTO>>> getContactSubjects() {
        return ResponseEntity.ok(ApiResponse.<List<ContactSubjectDTO>>builder()
                .success(true)
                .data(contentService.getActiveContactSubjects())
                .build());
    }

    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<FAQDTO>>> getFAQs(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.<List<FAQDTO>>builder()
                .success(true)
                .data(contentService.getActiveFAQs(category))
                .build());
    }

    @GetMapping("/testimonials")
    public ResponseEntity<ApiResponse<List<TestimonialDTO>>> getTestimonials(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.<List<TestimonialDTO>>builder()
                .success(true)
                .data(contentService.getActiveTestimonials(type))
                .build());
    }

    @GetMapping("/features")
    public ResponseEntity<ApiResponse<List<FeatureDTO>>> getFeatures() {
        return ResponseEntity.ok(ApiResponse.<List<FeatureDTO>>builder()
                .success(true)
                .data(contentService.getActiveFeatures())
                .build());
    }

    @GetMapping("/value-propositions")
    public ResponseEntity<ApiResponse<List<ValuePropositionDTO>>> getValuePropositions() {
        return ResponseEntity.ok(ApiResponse.<List<ValuePropositionDTO>>builder()
                .success(true)
                .data(contentService.getActiveValuePropositions())
                .build());
    }

    @GetMapping("/onboarding-steps")
    public ResponseEntity<ApiResponse<List<OnboardingStepDTO>>> getOnboardingSteps() {
        return ResponseEntity.ok(ApiResponse.<List<OnboardingStepDTO>>builder()
                .success(true)
                .data(contentService.getActiveOnboardingSteps())
                .build());
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SiteSettingsDTO>> getSiteSettings() {
        return ResponseEntity.ok(ApiResponse.<SiteSettingsDTO>builder()
                .success(true)
                .data(contentService.getSiteSettings())
                .build());
    }
}

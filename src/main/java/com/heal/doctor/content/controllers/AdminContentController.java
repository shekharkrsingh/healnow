package com.heal.doctor.content.controllers;

import com.heal.doctor.content.dto.*;
import com.heal.doctor.content.services.IContentService;
import com.heal.doctor.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private final IContentService contentService;

    @PutMapping("/statistics")
    public ResponseEntity<ApiResponse<Void>> updateStatistics(@RequestBody SiteStatisticsDTO dto) {
        contentService.updateStatistics(dto);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Statistics updated successfully")
                .build());
    }

    @PutMapping("/hero")
    public ResponseEntity<ApiResponse<Void>> updateHeroContent(@RequestBody HeroContentDTO dto) {
        contentService.updateHeroContent(dto);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Hero content updated successfully")
                .build());
    }

    @PutMapping("/contact-info")
    public ResponseEntity<ApiResponse<Void>> updateContactInfo(@RequestBody ContactInfoDTO dto) {
        contentService.updateContactInfo(dto);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Contact info updated successfully")
                .build());
    }

    @PostMapping("/contact-subjects")
    public ResponseEntity<ApiResponse<ContactSubjectDTO>> createContactSubject(@RequestBody ContactSubjectDTO dto) {
        return ResponseEntity.ok(ApiResponse.<ContactSubjectDTO>builder()
                .success(true)
                .data(contentService.createContactSubject(dto))
                .message("Contact subject created successfully")
                .build());
    }

    @PutMapping("/contact-subjects/{id}")
    public ResponseEntity<ApiResponse<ContactSubjectDTO>> updateContactSubject(@PathVariable String id, @RequestBody ContactSubjectDTO dto) {
        return ResponseEntity.ok(ApiResponse.<ContactSubjectDTO>builder()
                .success(true)
                .data(contentService.updateContactSubject(id, dto))
                .message("Contact subject updated successfully")
                .build());
    }

    @DeleteMapping("/contact-subjects/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContactSubject(@PathVariable String id) {
        contentService.deleteContactSubject(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Contact subject deleted successfully")
                .build());
    }

    @PostMapping("/faqs")
    public ResponseEntity<ApiResponse<FAQDTO>> createFAQ(@RequestBody FAQDTO dto) {
        return ResponseEntity.ok(ApiResponse.<FAQDTO>builder()
                .success(true)
                .data(contentService.createFAQ(dto))
                .message("FAQ created successfully")
                .build());
    }

    @PutMapping("/faqs/{id}")
    public ResponseEntity<ApiResponse<FAQDTO>> updateFAQ(@PathVariable String id, @RequestBody FAQDTO dto) {
        return ResponseEntity.ok(ApiResponse.<FAQDTO>builder()
                .success(true)
                .data(contentService.updateFAQ(id, dto))
                .message("FAQ updated successfully")
                .build());
    }

    @DeleteMapping("/faqs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFAQ(@PathVariable String id) {
        contentService.deleteFAQ(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("FAQ deleted successfully")
                .build());
    }

    @PostMapping("/testimonials")
    public ResponseEntity<ApiResponse<TestimonialDTO>> createTestimonial(@RequestBody TestimonialDTO dto) {
        return ResponseEntity.ok(ApiResponse.<TestimonialDTO>builder()
                .success(true)
                .data(contentService.createTestimonial(dto))
                .message("Testimonial created successfully")
                .build());
    }

    @PutMapping("/testimonials/{id}")
    public ResponseEntity<ApiResponse<TestimonialDTO>> updateTestimonial(@PathVariable String id, @RequestBody TestimonialDTO dto) {
        return ResponseEntity.ok(ApiResponse.<TestimonialDTO>builder()
                .success(true)
                .data(contentService.updateTestimonial(id, dto))
                .message("Testimonial updated successfully")
                .build());
    }

    @DeleteMapping("/testimonials/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTestimonial(@PathVariable String id) {
        contentService.deleteTestimonial(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Testimonial deleted successfully")
                .build());
    }

    @PostMapping("/features")
    public ResponseEntity<ApiResponse<FeatureDTO>> createFeature(@RequestBody FeatureDTO dto) {
        return ResponseEntity.ok(ApiResponse.<FeatureDTO>builder()
                .success(true)
                .data(contentService.createFeature(dto))
                .message("Feature created successfully")
                .build());
    }

    @PutMapping("/features/{id}")
    public ResponseEntity<ApiResponse<FeatureDTO>> updateFeature(@PathVariable String id, @RequestBody FeatureDTO dto) {
        return ResponseEntity.ok(ApiResponse.<FeatureDTO>builder()
                .success(true)
                .data(contentService.updateFeature(id, dto))
                .message("Feature updated successfully")
                .build());
    }

    @DeleteMapping("/features/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeature(@PathVariable String id) {
        contentService.deleteFeature(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Feature deleted successfully")
                .build());
    }

    @PostMapping("/value-propositions")
    public ResponseEntity<ApiResponse<ValuePropositionDTO>> createValueProposition(@RequestBody ValuePropositionDTO dto) {
        return ResponseEntity.ok(ApiResponse.<ValuePropositionDTO>builder()
                .success(true)
                .data(contentService.createValueProposition(dto))
                .message("Value proposition created successfully")
                .build());
    }

    @PutMapping("/value-propositions/{id}")
    public ResponseEntity<ApiResponse<ValuePropositionDTO>> updateValueProposition(@PathVariable String id, @RequestBody ValuePropositionDTO dto) {
        return ResponseEntity.ok(ApiResponse.<ValuePropositionDTO>builder()
                .success(true)
                .data(contentService.updateValueProposition(id, dto))
                .message("Value proposition updated successfully")
                .build());
    }

    @DeleteMapping("/value-propositions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteValueProposition(@PathVariable String id) {
        contentService.deleteValueProposition(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Value proposition deleted successfully")
                .build());
    }

    @PostMapping("/onboarding-steps")
    public ResponseEntity<ApiResponse<OnboardingStepDTO>> createOnboardingStep(@RequestBody OnboardingStepDTO dto) {
        return ResponseEntity.ok(ApiResponse.<OnboardingStepDTO>builder()
                .success(true)
                .data(contentService.createOnboardingStep(dto))
                .message("Onboarding step created successfully")
                .build());
    }

    @PutMapping("/onboarding-steps/{id}")
    public ResponseEntity<ApiResponse<OnboardingStepDTO>> updateOnboardingStep(@PathVariable String id, @RequestBody OnboardingStepDTO dto) {
        return ResponseEntity.ok(ApiResponse.<OnboardingStepDTO>builder()
                .success(true)
                .data(contentService.updateOnboardingStep(id, dto))
                .message("Onboarding step updated successfully")
                .build());
    }

    @DeleteMapping("/onboarding-steps/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOnboardingStep(@PathVariable String id) {
        contentService.deleteOnboardingStep(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Onboarding step deleted successfully")
                .build());
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> updateSiteSettings(@RequestBody SiteSettingsDTO dto) {
        contentService.updateSiteSettings(dto);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Site settings updated successfully")
                .build());
    }
}

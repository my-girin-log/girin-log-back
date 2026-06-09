package com.girinlog.persona.controller;

import com.girinlog.persona.controller.dto.SubmitOnboardingRequest;
import com.girinlog.persona.controller.dto.SubmitOnboardingResponse;
import com.girinlog.persona.service.OnboardingResult;
import com.girinlog.persona.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 온보딩 제출 → Persona 생성. (openapi POST /api/onboarding/submissions)
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitOnboardingResponse submit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitOnboardingRequest request) {
        OnboardingResult result = onboardingService.submit(
                Long.valueOf(jwt.getSubject()),
                request.blogUrl(),
                request.rawText(),
                request.toSurveyAnswers());
        return SubmitOnboardingResponse.from(result);
    }
}

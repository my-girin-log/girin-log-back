package com.girinlog.persona.controller.dto;

import com.girinlog.persona.service.OnboardingResult;

/**
 * 온보딩 제출 응답. (openapi SubmitOnboardingResponse)
 */
public record SubmitOnboardingResponse(Long personaId, boolean onboardingCompleted) {

    public static SubmitOnboardingResponse from(OnboardingResult result) {
        return new SubmitOnboardingResponse(result.personaId(), result.onboardingCompleted());
    }
}

package com.girinlog.persona.service;

/**
 * 온보딩 제출 결과(서비스 계층). 생성/갱신된 Persona id와 온보딩 완료 여부.
 */
public record OnboardingResult(Long personaId, boolean onboardingCompleted) {
}

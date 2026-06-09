package com.girinlog.persona.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.auth.service.UserService;
import com.girinlog.persona.domain.Persona;
import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.generation.PersonaGenerationInput;
import com.girinlog.persona.generation.PersonaGenerator;
import com.girinlog.persona.repository.OnboardingSurveyRepository;
import com.girinlog.persona.repository.PersonaRepository;
import com.girinlog.persona.repository.PersonaSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private OnboardingSurveyRepository onboardingSurveyRepository;
    @Mock
    private PersonaSourceRepository personaSourceRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private PersonaGenerator personaGenerator;
    @Mock
    private UserService userService;

    private OnboardingService onboardingService() {
        return new OnboardingService(
                onboardingSurveyRepository,
                personaSourceRepository,
                personaRepository,
                personaGenerator,
                userService,
                new ObjectMapper());
    }

    private GeneratedPersona sampleGenerated() {
        return new GeneratedPersona("담백한", "사건→감정→판단", List.of("회고"),
                "핵심부터", "배운 점", "짧은 글", "요약", "# Persona");
    }

    @Test
    @DisplayName("처음 온보딩이면 Persona를 생성하고 사용자 온보딩을 완료한다")
    void submitCreatesPersonaAndCompletesOnboarding() {
        when(personaGenerator.generate(any())).thenReturn(sampleGenerated());
        when(personaRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingResult result = onboardingService().submit(
                1L, null, null,
                List.of(new PersonaGenerationInput.SurveyAnswer(1, "순서대로 정리")));

        assertThat(result.onboardingCompleted()).isTrue();
        verify(onboardingSurveyRepository).save(any());
        verify(personaSourceRepository).save(any()); // 최소 SURVEY 1건
        verify(personaRepository).save(any(Persona.class));
        verify(userService).completeOnboarding(1L);
    }

    @Test
    @DisplayName("이미 Persona가 있으면 새로 저장하지 않고 갱신한다")
    void submitRefreshesExistingPersona() {
        Persona existing = Persona.create(1L, sampleGenerated());
        when(personaGenerator.generate(any())).thenReturn(sampleGenerated());
        when(personaRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        onboardingService().submit(
                1L, "https://blog", "기존 글",
                List.of(new PersonaGenerationInput.SurveyAnswer(1, "답")));

        verify(personaRepository, org.mockito.Mockito.never()).save(any(Persona.class));
        verify(userService).completeOnboarding(1L);
    }
}

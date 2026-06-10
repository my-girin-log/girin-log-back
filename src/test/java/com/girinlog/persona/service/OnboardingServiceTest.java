package com.girinlog.persona.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.auth.service.UserService;
import com.girinlog.event.service.EventLogRecorder;
import com.girinlog.persona.analysis.BlogAnalyzer;
import com.girinlog.persona.domain.AnalysisStatus;
import com.girinlog.persona.domain.Persona;
import com.girinlog.persona.domain.PersonaSource;
import com.girinlog.persona.domain.SourceType;
import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.generation.PersonaGenerationInput;
import com.girinlog.persona.generation.PersonaGenerator;
import com.girinlog.persona.repository.OnboardingSurveyRepository;
import com.girinlog.persona.repository.PersonaRepository;
import com.girinlog.persona.repository.PersonaSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
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
    private BlogAnalyzer blogAnalyzer;
    @Mock
    private UserService userService;
    @Mock
    private EventLogRecorder eventLogRecorder;

    private OnboardingService onboardingService() {
        return new OnboardingService(
                onboardingSurveyRepository,
                personaSourceRepository,
                personaRepository,
                personaGenerator,
                blogAnalyzer,
                userService,
                eventLogRecorder,
                new ObjectMapper());
    }

    private GeneratedPersona sampleGenerated() {
        return new GeneratedPersona("담백한", "사건→감정→판단", List.of("회고"),
                "핵심부터", "배운 점", "짧은 글", "요약", "# Persona");
    }

    private void stubPersonaCreation() {
        when(personaGenerator.generate(any())).thenReturn(sampleGenerated());
        when(personaRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            ReflectionTestUtils.setField(persona, "id", 100L);
            return persona;
        });
    }

    private PersonaSource savedBlogSource() {
        ArgumentCaptor<PersonaSource> captor = ArgumentCaptor.forClass(PersonaSource.class);
        verify(personaSourceRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(source -> source.sourceType() == SourceType.BLOG_URL)
                .findFirst()
                .orElseThrow();
    }

    private PersonaGenerationInput capturedInput() {
        ArgumentCaptor<PersonaGenerationInput> captor = ArgumentCaptor.forClass(PersonaGenerationInput.class);
        verify(personaGenerator).generate(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("처음 온보딩이면 Persona를 생성하고 사용자 온보딩을 완료한다 (블로그 없음 → 분석 안 함)")
    void submitCreatesPersonaAndCompletesOnboarding() {
        stubPersonaCreation();

        OnboardingResult result = onboardingService().submit(
                1L, null, null,
                List.of(new PersonaGenerationInput.SurveyAnswer(1, "순서대로 정리")));

        assertThat(result.onboardingCompleted()).isTrue();
        verify(onboardingSurveyRepository).save(any());
        verify(personaSourceRepository).save(any()); // SURVEY 1건
        verify(userService).completeOnboarding(1L);
        verify(blogAnalyzer, never()).fetchReadableText(any());
    }

    @Test
    @DisplayName("블로그 분석 성공: source COMPLETED + 본문을 생성 입력에 넣는다")
    void submitWithBlogAnalysisSuccess() {
        when(personaSourceRepository.save(any(PersonaSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(blogAnalyzer.fetchReadableText("https://blog.example.com/a"))
                .thenReturn(Optional.of("블로그 본문 텍스트"));
        stubPersonaCreation();

        onboardingService().submit(
                1L, "https://blog.example.com/a", null,
                List.of(new PersonaGenerationInput.SurveyAnswer(1, "답")));

        assertThat(capturedInput().blogContent()).isEqualTo("블로그 본문 텍스트");
        assertThat(savedBlogSource().analysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        verify(userService).completeOnboarding(1L);
    }

    @Test
    @DisplayName("블로그 분석 실패: source FAILED, 본문 없음, 온보딩은 진행")
    void submitWithBlogAnalysisFailure() {
        when(personaSourceRepository.save(any(PersonaSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(blogAnalyzer.fetchReadableText("https://blog.example.com/b"))
                .thenReturn(Optional.empty());
        stubPersonaCreation();

        OnboardingResult result = onboardingService().submit(
                1L, "https://blog.example.com/b", null,
                List.of(new PersonaGenerationInput.SurveyAnswer(1, "답")));

        assertThat(result.onboardingCompleted()).isTrue();
        assertThat(capturedInput().blogContent()).isNull();
        assertThat(savedBlogSource().analysisStatus()).isEqualTo(AnalysisStatus.FAILED);
    }

    @Test
    @DisplayName("이미 Persona가 있으면 새로 저장하지 않고 갱신한다")
    void submitRefreshesExistingPersona() {
        Persona existing = Persona.create(1L, sampleGenerated());
        ReflectionTestUtils.setField(existing, "id", 100L);
        when(personaSourceRepository.save(any(PersonaSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(blogAnalyzer.fetchReadableText(any())).thenReturn(Optional.empty());
        when(personaGenerator.generate(any())).thenReturn(sampleGenerated());
        when(personaRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        onboardingService().submit(
                1L, "https://blog", "기존 글",
                List.of(new PersonaGenerationInput.SurveyAnswer(1, "답")));

        verify(personaRepository, never()).save(any(Persona.class));
        verify(userService).completeOnboarding(1L);
    }
}

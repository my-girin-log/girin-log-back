package com.girinlog.persona.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.auth.service.UserService;
import com.girinlog.common.error.BusinessException;
import com.girinlog.event.domain.EventType;
import com.girinlog.event.service.EventLogRecorder;
import com.girinlog.persona.analysis.BlogAnalyzer;
import com.girinlog.persona.domain.OnboardingSurvey;
import com.girinlog.persona.domain.Persona;
import com.girinlog.persona.domain.PersonaSource;
import com.girinlog.persona.exception.PersonaErrorCode;
import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.generation.PersonaGenerationInput;
import com.girinlog.persona.generation.PersonaGenerator;
import com.girinlog.persona.repository.OnboardingSurveyRepository;
import com.girinlog.persona.repository.PersonaRepository;
import com.girinlog.persona.repository.PersonaSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 온보딩 제출 흐름: 설문/원천 저장 → (블로그 분석) → Persona 생성(포트) → User 온보딩 완료.
 * 일부 입력만 있어도, 블로그 분석이 실패해도 Persona를 생성한다(product 2-3, 비차단).
 */
@Service
@Transactional
public class OnboardingService {

    private final OnboardingSurveyRepository onboardingSurveyRepository;
    private final PersonaSourceRepository personaSourceRepository;
    private final PersonaRepository personaRepository;
    private final PersonaGenerator personaGenerator;
    private final BlogAnalyzer blogAnalyzer;
    private final UserService userService;
    private final EventLogRecorder eventLogRecorder;
    private final ObjectMapper objectMapper;

    public OnboardingService(
            OnboardingSurveyRepository onboardingSurveyRepository,
            PersonaSourceRepository personaSourceRepository,
            PersonaRepository personaRepository,
            PersonaGenerator personaGenerator,
            BlogAnalyzer blogAnalyzer,
            UserService userService,
            EventLogRecorder eventLogRecorder,
            ObjectMapper objectMapper) {
        this.onboardingSurveyRepository = onboardingSurveyRepository;
        this.personaSourceRepository = personaSourceRepository;
        this.personaRepository = personaRepository;
        this.personaGenerator = personaGenerator;
        this.blogAnalyzer = blogAnalyzer;
        this.userService = userService;
        this.eventLogRecorder = eventLogRecorder;
        this.objectMapper = objectMapper;
    }

    public OnboardingResult submit(
            Long userId, String blogUrl, String rawText, List<PersonaGenerationInput.SurveyAnswer> answers) {
        String answersJson = serialize(answers);

        onboardingSurveyRepository.save(OnboardingSurvey.of(userId, answersJson));
        String blogContent = analyzeBlogIfPresent(userId, blogUrl);
        saveTextAndSurveySources(userId, rawText, answersJson);

        GeneratedPersona generated = personaGenerator.generate(
                new PersonaGenerationInput(blogUrl, blogContent, rawText, answers));
        Persona persona = personaRepository.findByUserId(userId)
                .map(existing -> {
                    existing.refresh(generated);
                    return existing;
                })
                .orElseGet(() -> personaRepository.save(Persona.create(userId, generated)));

        userService.completeOnboarding(userId);
        eventLogRecorder.record(userId, EventType.PERSONA_CREATED, Map.of("personaId", persona.id()));
        eventLogRecorder.record(userId, EventType.ONBOARDING_COMPLETED, Map.of("personaId", persona.id()));
        return new OnboardingResult(persona.id(), true);
    }

    /**
     * 블로그 링크가 있으면 PersonaSource를 만들고 본문을 추출한다.
     * 성공 시 COMPLETED + 본문 반환, 실패 시 FAILED + null(생성은 계속).
     */
    private String analyzeBlogIfPresent(Long userId, String blogUrl) {
        if (blogUrl == null || blogUrl.isBlank()) {
            return null;
        }
        PersonaSource source = personaSourceRepository.save(PersonaSource.blogUrl(userId, blogUrl));
        source.markAnalyzing();

        Optional<String> content = blogAnalyzer.fetchReadableText(blogUrl);
        if (content.isPresent()) {
            source.markCompleted();
            return content.get();
        }
        source.markFailed();
        return null;
    }

    private void saveTextAndSurveySources(Long userId, String rawText, String answersJson) {
        if (rawText != null && !rawText.isBlank()) {
            personaSourceRepository.save(PersonaSource.text(userId, rawText));
        }
        personaSourceRepository.save(PersonaSource.survey(userId, answersJson));
    }

    private String serialize(List<PersonaGenerationInput.SurveyAnswer> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(PersonaErrorCode.ONBOARDING_PERSIST_FAILED);
        }
    }
}

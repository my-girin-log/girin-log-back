package com.girinlog.persona.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.auth.service.UserService;
import com.girinlog.common.error.BusinessException;
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

/**
 * 온보딩 제출 흐름: 설문/원천 저장 → Persona 생성(포트) → User 온보딩 완료.
 * 일부 입력만 있어도 Persona를 생성한다(product 2-3).
 */
@Service
@Transactional
public class OnboardingService {

    private final OnboardingSurveyRepository onboardingSurveyRepository;
    private final PersonaSourceRepository personaSourceRepository;
    private final PersonaRepository personaRepository;
    private final PersonaGenerator personaGenerator;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public OnboardingService(
            OnboardingSurveyRepository onboardingSurveyRepository,
            PersonaSourceRepository personaSourceRepository,
            PersonaRepository personaRepository,
            PersonaGenerator personaGenerator,
            UserService userService,
            ObjectMapper objectMapper) {
        this.onboardingSurveyRepository = onboardingSurveyRepository;
        this.personaSourceRepository = personaSourceRepository;
        this.personaRepository = personaRepository;
        this.personaGenerator = personaGenerator;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public OnboardingResult submit(
            Long userId, String blogUrl, String rawText, List<PersonaGenerationInput.SurveyAnswer> answers) {
        String answersJson = serialize(answers);

        onboardingSurveyRepository.save(OnboardingSurvey.of(userId, answersJson));
        saveSources(userId, blogUrl, rawText, answersJson);

        GeneratedPersona generated = personaGenerator.generate(
                new PersonaGenerationInput(blogUrl, rawText, answers));
        Persona persona = personaRepository.findByUserId(userId)
                .map(existing -> {
                    existing.refresh(generated);
                    return existing;
                })
                .orElseGet(() -> personaRepository.save(Persona.create(userId, generated)));

        userService.completeOnboarding(userId);
        return new OnboardingResult(persona.id(), true);
    }

    private void saveSources(Long userId, String blogUrl, String rawText, String answersJson) {
        if (blogUrl != null && !blogUrl.isBlank()) {
            personaSourceRepository.save(PersonaSource.blogUrl(userId, blogUrl));
        }
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

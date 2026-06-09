package com.girinlog.persona.controller.dto;

import com.girinlog.persona.generation.PersonaGenerationInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 온보딩 제출 요청. (openapi SubmitOnboardingRequest)
 * blogUrl/rawText는 선택, answers는 1~10개.
 */
public record SubmitOnboardingRequest(
        String blogUrl,
        String rawText,
        @NotEmpty
        @Size(max = 10)
        @Valid
        List<Answer> answers) {

    public record Answer(
            @Min(1) @Max(10) int questionId,
            @NotBlank String answer) {
    }

    /** 도메인 생성 입력으로 변환한다. */
    public List<PersonaGenerationInput.SurveyAnswer> toSurveyAnswers() {
        return answers.stream()
                .map(answer -> new PersonaGenerationInput.SurveyAnswer(answer.questionId(), answer.answer()))
                .toList();
    }
}

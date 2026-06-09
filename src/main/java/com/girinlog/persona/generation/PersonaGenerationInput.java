package com.girinlog.persona.generation;

import java.util.List;

/**
 * Persona 생성 입력. 블로그 링크/원문/설문 응답을 함께 넘기되 일부만 있어도 생성 가능하다.
 */
public record PersonaGenerationInput(
        String blogUrl,
        String rawText,
        List<SurveyAnswer> answers) {

    public record SurveyAnswer(int questionId, String answer) {
    }
}

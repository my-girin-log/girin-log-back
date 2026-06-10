package com.girinlog.persona.generation;

import java.util.List;

/**
 * Persona 생성 입력. 블로그 링크/블로그 본문/원문/설문 응답을 함께 넘기되 일부만 있어도 생성 가능하다.
 * {@code blogContent}는 블로그 링크를 분석해 추출한 본문(없거나 분석 실패 시 null).
 */
public record PersonaGenerationInput(
        String blogUrl,
        String blogContent,
        String rawText,
        List<SurveyAnswer> answers) {

    public record SurveyAnswer(int questionId, String answer) {
    }
}

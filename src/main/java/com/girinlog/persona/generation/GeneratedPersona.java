package com.girinlog.persona.generation;

import java.util.List;

/**
 * Persona 생성기의 출력. 도메인은 생성 방식(LLM 등)을 알지 못한다.
 */
public record GeneratedPersona(
        String tone,
        String thinkingStyle,
        List<String> recurringInterests,
        String organizingHabit,
        String retrospectionCriteria,
        String preferredStructure,
        String summary,
        String markdown) {
}

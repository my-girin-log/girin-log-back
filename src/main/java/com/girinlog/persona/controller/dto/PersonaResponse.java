package com.girinlog.persona.controller.dto;

import com.girinlog.common.time.ServiceClockConfig;
import com.girinlog.persona.domain.Persona;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Persona 조회 응답. (openapi Persona 스키마) 시각은 KST 오프셋으로 내려준다.
 */
public record PersonaResponse(
        Long id,
        Long userId,
        String tone,
        String thinkingStyle,
        List<String> recurringInterests,
        String organizingHabit,
        String retrospectionCriteria,
        String preferredStructure,
        String summary,
        String markdown,
        OffsetDateTime lastRefreshedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static PersonaResponse from(Persona persona) {
        return new PersonaResponse(
                persona.id(),
                persona.userId(),
                persona.tone(),
                persona.thinkingStyle(),
                persona.recurringInterests(),
                persona.organizingHabit(),
                persona.retrospectionCriteria(),
                persona.preferredStructure(),
                persona.summary(),
                persona.markdown(),
                toKst(persona.lastRefreshedAt()),
                toKst(persona.createdAt()),
                toKst(persona.updatedAt()));
    }

    private static OffsetDateTime toKst(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ServiceClockConfig.KST).toOffsetDateTime();
    }
}

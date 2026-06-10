package com.girinlog.retrospective.controller;

import com.girinlog.retrospective.domain.Retrospective;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RetrospectiveResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> sourceDailyChatSessionIds,
        String title,
        String markdown,
        OffsetDateTime createdAt
) {

    public static RetrospectiveResponse from(Retrospective retrospective) {
        return new RetrospectiveResponse(
                retrospective.id(),
                retrospective.periodStart(),
                retrospective.periodEnd(),
                retrospective.sourceDailyChatSessionIds(),
                retrospective.title(),
                retrospective.markdown(),
                retrospective.createdAt()
        );
    }
}

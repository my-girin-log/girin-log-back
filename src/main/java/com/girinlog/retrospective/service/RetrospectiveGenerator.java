package com.girinlog.retrospective.service;

import com.girinlog.conversation.domain.DailyChatSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RetrospectiveGenerator {

    GeneratedRetrospective generate(
            LocalDate startDate,
            LocalDate endDate,
            List<DailyChatSession> sessions,
            Optional<String> personaMarkdown
    );
}

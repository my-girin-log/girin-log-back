package com.girinlog.retrospective.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.DailyChatSessionStatus;
import com.girinlog.conversation.repository.DailyChatSessionRepository;
import com.girinlog.event.domain.EventType;
import com.girinlog.event.service.EventLogRecorder;
import com.girinlog.persona.service.PersonaService;
import com.girinlog.retrospective.RetrospectiveErrorCode;
import com.girinlog.retrospective.domain.Retrospective;
import com.girinlog.retrospective.repository.RetrospectiveRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RetrospectiveService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final RetrospectiveRepository retrospectiveRepository;
    private final DailyChatSessionRepository dailyChatSessionRepository;
    private final PersonaService personaService;
    private final RetrospectiveGenerator retrospectiveGenerator;
    private final EventLogRecorder eventLogRecorder;
    private final Clock clock;

    public RetrospectiveService(
            RetrospectiveRepository retrospectiveRepository,
            DailyChatSessionRepository dailyChatSessionRepository,
            PersonaService personaService,
            RetrospectiveGenerator retrospectiveGenerator,
            EventLogRecorder eventLogRecorder,
            Clock clock
    ) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.dailyChatSessionRepository = dailyChatSessionRepository;
        this.personaService = personaService;
        this.retrospectiveGenerator = retrospectiveGenerator;
        this.eventLogRecorder = eventLogRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RetrospectivePage listRetrospectives(Long userId, String cursor, Integer limit) {
        int pageSize = normalizeLimit(limit);
        Long cursorId = RetrospectiveCursor.decode(cursor);
        List<Retrospective> retrospectives = retrospectiveRepository.findPage(
                userId,
                cursorId == null ? Long.MAX_VALUE : cursorId,
                PageRequest.of(0, pageSize + 1)
        );
        if (retrospectives.size() <= pageSize) {
            return new RetrospectivePage(retrospectives, null);
        }

        List<Retrospective> items = retrospectives.subList(0, pageSize);
        String nextCursor = RetrospectiveCursor.encode(items.getLast().id());
        return new RetrospectivePage(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public Retrospective getRetrospective(Long userId, Long retrospectiveId) {
        return retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)
                .orElseThrow(() -> new BusinessException(RetrospectiveErrorCode.RETROSPECTIVE_NOT_FOUND));
    }

    @Transactional
    public Retrospective createRetrospective(Long userId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        List<DailyChatSession> sessions = dailyChatSessionRepository
                .findByUserIdAndServiceDateBetweenAndStatusOrderByServiceDateAscCreatedAtAsc(
                        userId,
                        startDate,
                        endDate,
                        DailyChatSessionStatus.ENDED
                );
        if (sessions.isEmpty()) {
            throw new BusinessException(RetrospectiveErrorCode.NO_RETROSPECTIVE_SOURCE_SESSION);
        }

        GeneratedRetrospective generated = retrospectiveGenerator.generate(
                startDate,
                endDate,
                sessions,
                personaService.findPersonaMarkdown(userId)
        );
        Retrospective retrospective = Retrospective.create(
                userId,
                startDate,
                endDate,
                sessions.stream().map(DailyChatSession::id).toList(),
                generated.title(),
                generated.markdown(),
                OffsetDateTime.now(clock)
        );
        Retrospective savedRetrospective = retrospectiveRepository.save(retrospective);
        eventLogRecorder.record(userId, EventType.RETROSPECTIVE_CREATED, metadata()
                .add("retrospectiveId", savedRetrospective.id())
                .add("startDate", savedRetrospective.periodStart().toString())
                .add("endDate", savedRetrospective.periodEnd().toString())
                .add("dailyChatSessionIds", savedRetrospective.sourceDailyChatSessionIds())
                .toMap());
        return savedRetrospective;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(RetrospectiveErrorCode.INVALID_RETROSPECTIVE_DATE_RANGE);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(
                    RetrospectiveErrorCode.INVALID_RETROSPECTIVE_DATE_RANGE,
                    "limit은 1 이상 100 이하여야 합니다."
            );
        }
        return limit;
    }

    private Metadata metadata() {
        return new Metadata();
    }

    private static class Metadata {

        private final Map<String, Object> values = new LinkedHashMap<>();

        Metadata add(String key, Object value) {
            values.put(key, value);
            return this;
        }

        Map<String, Object> toMap() {
            return values;
        }
    }
}

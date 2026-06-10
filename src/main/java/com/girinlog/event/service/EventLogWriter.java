package com.girinlog.event.service;

import com.girinlog.event.domain.EventLog;
import com.girinlog.event.domain.EventType;
import com.girinlog.event.repository.EventLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
class EventLogWriter {

    private final EventLogRepository eventLogRepository;
    private final Clock clock;

    EventLogWriter(EventLogRepository eventLogRepository, Clock clock) {
        this.eventLogRepository = eventLogRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Long userId, EventType eventType, String metadataJson) {
        eventLogRepository.save(EventLog.create(userId, eventType, OffsetDateTime.now(clock), metadataJson));
    }
}

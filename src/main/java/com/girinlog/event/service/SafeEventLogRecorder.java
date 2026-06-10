package com.girinlog.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.event.domain.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class SafeEventLogRecorder implements EventLogRecorder {

    private static final Logger log = LoggerFactory.getLogger(SafeEventLogRecorder.class);

    private final EventLogWriter eventLogWriter;
    private final ObjectMapper objectMapper;

    SafeEventLogRecorder(EventLogWriter eventLogWriter, ObjectMapper objectMapper) {
        this.eventLogWriter = eventLogWriter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(Long userId, EventType eventType, Map<String, Object> metadata) {
        try {
            eventLogWriter.write(userId, eventType, toJson(metadata));
        } catch (RuntimeException exception) {
            log.warn("event log record failed eventType={} userId={}", eventType, userId, exception);
        }
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("EventLog metadata 직렬화에 실패했습니다.", exception);
        }
    }
}

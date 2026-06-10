package com.girinlog.event.service;

import com.girinlog.event.domain.EventType;

import java.util.Map;

public interface EventLogRecorder {

    void record(Long userId, EventType eventType, Map<String, Object> metadata);
}

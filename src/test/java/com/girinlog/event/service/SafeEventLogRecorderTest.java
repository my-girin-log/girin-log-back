package com.girinlog.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.event.domain.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class SafeEventLogRecorderTest {

    @Test
    @DisplayName("EventLog 저장 실패는 본 흐름으로 전파하지 않는다")
    void record_does_not_propagate_event_log_write_failure() {
        EventLogWriter eventLogWriter = mock(EventLogWriter.class);
        SafeEventLogRecorder recorder = new SafeEventLogRecorder(eventLogWriter, new ObjectMapper());
        doThrow(new IllegalStateException("event log db failure"))
                .when(eventLogWriter)
                .write(1L, EventType.MEMO_CREATED, "{\"memoId\":10}");

        assertThatCode(() -> recorder.record(1L, EventType.MEMO_CREATED, Map.of("memoId", 10L)))
                .doesNotThrowAnyException();

        then(eventLogWriter).should().write(1L, EventType.MEMO_CREATED, "{\"memoId\":10}");
    }
}

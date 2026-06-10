package com.girinlog.event.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventLogTest {

    @Test
    @DisplayName("EventLog는 사용자, 이벤트 타입, 발생 시각, metadataJson을 보관한다")
    void event_log_holds_append_only_event_data() {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-06-08T09:00:00+09:00");

        EventLog eventLog = EventLog.create(1L, EventType.MEMO_CREATED, occurredAt, "{\"memoId\":10}");

        assertThat(eventLog.userId()).isEqualTo(1L);
        assertThat(eventLog.eventType()).isEqualTo(EventType.MEMO_CREATED);
        assertThat(eventLog.occurredAt()).isEqualTo(occurredAt);
        assertThat(eventLog.metadataJson()).isEqualTo("{\"memoId\":10}");
    }
}

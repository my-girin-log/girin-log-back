package com.girinlog.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "event_logs")
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    @Column(nullable = false, columnDefinition = "text")
    private String metadataJson;

    protected EventLog() {
    }

    private EventLog(Long userId, EventType eventType, OffsetDateTime occurredAt, String metadataJson) {
        this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
        this.eventType = Objects.requireNonNull(eventType, "eventType은 필수입니다.");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt은 필수입니다.");
        this.metadataJson = Objects.requireNonNull(metadataJson, "metadataJson은 필수입니다.");
    }

    public static EventLog create(Long userId, EventType eventType, OffsetDateTime occurredAt, String metadataJson) {
        return new EventLog(userId, eventType, occurredAt, metadataJson);
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public EventType eventType() {
        return eventType;
    }

    public OffsetDateTime occurredAt() {
        return occurredAt;
    }

    public String metadataJson() {
        return metadataJson;
    }
}

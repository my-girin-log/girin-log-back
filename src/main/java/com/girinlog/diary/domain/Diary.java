package com.girinlog.diary.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "diaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_diaries_user_service_date",
                columnNames = {"user_id", "service_date"}
        )
)
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "diary_main_events", joinColumns = @JoinColumn(name = "diary_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "main_event", nullable = false, columnDefinition = "text")
    private List<String> mainEvents = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String emotionContext;

    @Column(columnDefinition = "text")
    private String concerns;

    @Column(columnDefinition = "text")
    private String newCriteria;

    @Column(columnDefinition = "text")
    private String nextActions;

    @Column(nullable = false, columnDefinition = "text")
    private String markdown;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected Diary() {
    }

    private Diary(
            Long userId,
            LocalDate serviceDate,
            String summary,
            List<String> mainEvents,
            String emotionContext,
            String concerns,
            String newCriteria,
            String nextActions,
            String markdown,
            OffsetDateTime createdAt
    ) {
        this.userId = userId;
        this.serviceDate = serviceDate;
        this.summary = summary;
        this.mainEvents = new ArrayList<>(mainEvents);
        this.emotionContext = emotionContext;
        this.concerns = concerns;
        this.newCriteria = newCriteria;
        this.nextActions = nextActions;
        this.markdown = markdown;
        this.createdAt = createdAt;
    }

    public static Diary create(
            Long userId,
            LocalDate serviceDate,
            String summary,
            List<String> mainEvents,
            String emotionContext,
            String concerns,
            String newCriteria,
            String nextActions,
            String markdown,
            OffsetDateTime createdAt
    ) {
        return new Diary(
                userId,
                serviceDate,
                requireText(summary, "summary"),
                requireMainEvents(mainEvents),
                emotionContext,
                concerns,
                newCriteria,
                nextActions,
                requireText(markdown, "markdown"),
                createdAt
        );
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public LocalDate serviceDate() {
        return serviceDate;
    }

    public String summary() {
        return summary;
    }

    public List<String> mainEvents() {
        return Collections.unmodifiableList(mainEvents);
    }

    public String emotionContext() {
        return emotionContext;
    }

    public String concerns() {
        return concerns;
    }

    public String newCriteria() {
        return newCriteria;
    }

    public String nextActions() {
        return nextActions;
    }

    public String markdown() {
        return markdown;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "는 비어 있을 수 없습니다.");
        }
        return value;
    }

    private static List<String> requireMainEvents(List<String> mainEvents) {
        if (mainEvents == null) {
            return List.of();
        }
        return mainEvents.stream()
                .map(event -> requireText(event, "mainEvent"))
                .toList();
    }
}

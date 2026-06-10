package com.girinlog.retrospective.domain;

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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "retrospectives")
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "retrospective_source_session_ids", joinColumns = @JoinColumn(name = "retrospective_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "daily_chat_session_id", nullable = false)
    private List<Long> sourceDailyChatSessionIds = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String markdown;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected Retrospective() {
    }

    private Retrospective(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd,
            List<Long> sourceDailyChatSessionIds,
            String title,
            String markdown,
            OffsetDateTime createdAt
    ) {
        this.userId = userId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.sourceDailyChatSessionIds = new ArrayList<>(sourceDailyChatSessionIds);
        this.title = title;
        this.markdown = markdown;
        this.createdAt = createdAt;
    }

    public static Retrospective create(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd,
            List<Long> sourceDailyChatSessionIds,
            String title,
            String markdown,
            OffsetDateTime createdAt
    ) {
        return new Retrospective(
                userId,
                periodStart,
                periodEnd,
                requireSourceSessionIds(sourceDailyChatSessionIds),
                requireText(title, "title"),
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

    public LocalDate periodStart() {
        return periodStart;
    }

    public LocalDate periodEnd() {
        return periodEnd;
    }

    public List<Long> sourceDailyChatSessionIds() {
        return Collections.unmodifiableList(sourceDailyChatSessionIds);
    }

    public String title() {
        return title;
    }

    public String markdown() {
        return markdown;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    private static List<Long> requireSourceSessionIds(List<Long> sourceDailyChatSessionIds) {
        if (sourceDailyChatSessionIds == null || sourceDailyChatSessionIds.isEmpty()) {
            throw new IllegalArgumentException("sourceDailyChatSessionIds는 비어 있을 수 없습니다.");
        }
        return sourceDailyChatSessionIds;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "는 비어 있을 수 없습니다.");
        }
        return value;
    }
}

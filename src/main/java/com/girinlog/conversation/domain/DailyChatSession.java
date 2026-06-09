package com.girinlog.conversation.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "daily_chat_sessions")
public class DailyChatSession {

    public static final int DEFAULT_MAX_FOLLOW_UP_COUNT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DailyChatSessionStatus status;

    @Column(nullable = false)
    private int followUpCount;

    @Column(nullable = false)
    private int maxFollowUpCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_chat_session_memo_summary_ids", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "memo_summary_id", nullable = false)
    private List<Long> memoSummaryIds = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "text")
    private String selectedSummariesSnapshot;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_chat_session_conversation", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "turn_order")
    private List<ConversationTurn> conversation = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EndedReason endedReason;

    @Column(columnDefinition = "text")
    private String closingMessage;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime endedAt;

    protected DailyChatSession() {
    }

    private DailyChatSession(
            Long userId,
            LocalDate serviceDate,
            List<Long> memoSummaryIds,
            String selectedSummariesSnapshot,
            int maxFollowUpCount,
            OffsetDateTime createdAt
    ) {
        this.userId = userId;
        this.serviceDate = serviceDate;
        this.status = DailyChatSessionStatus.OPEN;
        this.followUpCount = 0;
        this.maxFollowUpCount = maxFollowUpCount;
        this.memoSummaryIds = new ArrayList<>(memoSummaryIds);
        this.selectedSummariesSnapshot = selectedSummariesSnapshot;
        this.createdAt = createdAt;
    }

    public static DailyChatSession start(
            Long userId,
            LocalDate serviceDate,
            List<Long> memoSummaryIds,
            String selectedSummariesSnapshot,
            String firstFollowUpQuestion,
            OffsetDateTime createdAt
    ) {
        DailyChatSession session = new DailyChatSession(
                userId,
                serviceDate,
                memoSummaryIds,
                selectedSummariesSnapshot,
                DEFAULT_MAX_FOLLOW_UP_COUNT,
                createdAt
        );
        session.addSilokFollowUpQuestion(firstFollowUpQuestion, createdAt);
        return session;
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

    public DailyChatSessionStatus status() {
        return status;
    }

    public int followUpCount() {
        return followUpCount;
    }

    public int maxFollowUpCount() {
        return maxFollowUpCount;
    }

    public List<Long> memoSummaryIds() {
        return Collections.unmodifiableList(memoSummaryIds);
    }

    public List<ConversationTurn> conversation() {
        return Collections.unmodifiableList(conversation);
    }

    public EndedReason endedReason() {
        return endedReason;
    }

    public String closingMessage() {
        return closingMessage;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime endedAt() {
        return endedAt;
    }

    public boolean isEnded() {
        return status == DailyChatSessionStatus.ENDED;
    }

    public boolean canAskMore() {
        return followUpCount < maxFollowUpCount;
    }

    public void addUserAnswer(String content, OffsetDateTime answeredAt) {
        conversation.add(ConversationTurn.user(content, answeredAt));
    }

    public void addSilokFollowUpQuestion(String content, OffsetDateTime askedAt) {
        if (!canAskMore()) {
            throw new IllegalStateException("역질문 상한을 초과할 수 없습니다.");
        }
        conversation.add(ConversationTurn.silok(content, askedAt));
        followUpCount++;
    }

    public void end(EndedReason reason, String closingMessage, OffsetDateTime endedAt) {
        this.status = DailyChatSessionStatus.ENDED;
        this.endedReason = reason;
        this.closingMessage = closingMessage;
        this.endedAt = endedAt;
        this.conversation.add(ConversationTurn.silok(closingMessage, endedAt));
    }
}

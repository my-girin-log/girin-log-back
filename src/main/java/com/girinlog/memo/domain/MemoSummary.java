package com.girinlog.memo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "memo_summaries")
public class MemoSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private String categoryName;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @OneToMany(mappedBy = "memoSummary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemoSummaryItem> items = new ArrayList<>();

    @Column(nullable = false)
    private boolean chatAvailable;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MemoSummaryChatDisabledReason chatDisabledReason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected MemoSummary() {
    }

    private MemoSummary(
            Long userId,
            LocalDate serviceDate,
            String categoryName,
            String summary,
            OffsetDateTime createdAt
    ) {
        this.userId = userId;
        this.serviceDate = serviceDate;
        this.categoryName = categoryName;
        this.summary = summary;
        this.chatAvailable = true;
        this.createdAt = createdAt;
    }

    public static MemoSummary create(
            Long userId,
            LocalDate serviceDate,
            String categoryName,
            String summary,
            List<MemoSummaryItem> items,
            OffsetDateTime createdAt
    ) {
        MemoSummary memoSummary = new MemoSummary(userId, serviceDate, categoryName, summary, createdAt);
        items.forEach(memoSummary::addItem);
        return memoSummary;
    }

    private void addItem(MemoSummaryItem item) {
        item.assignTo(this);
        items.add(item);
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

    public String categoryName() {
        return categoryName;
    }

    public String summary() {
        return summary;
    }

    public int itemCount() {
        return items.size();
    }

    public List<MemoSummaryItem> items() {
        return Collections.unmodifiableList(items);
    }

    public void disableChat() {
        this.chatAvailable = false;
        this.chatDisabledReason = MemoSummaryChatDisabledReason.ALREADY_CHATTED;
    }

    public boolean chatAvailable() {
        return chatAvailable;
    }

    public MemoSummaryChatDisabledReason chatDisabledReason() {
        return chatDisabledReason;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }
}

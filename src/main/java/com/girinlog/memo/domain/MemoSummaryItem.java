package com.girinlog.memo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "memo_summary_items")
public class MemoSummaryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_summary_id", nullable = false)
    private MemoSummary memoSummary;

    @Column(nullable = false)
    private Long memoId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    protected MemoSummaryItem() {
    }

    public MemoSummaryItem(Long memoId, String content) {
        this.memoId = memoId;
        this.content = content;
    }

    void assignTo(MemoSummary memoSummary) {
        this.memoSummary = memoSummary;
    }

    public Long id() {
        return id;
    }

    public Long memoId() {
        return memoId;
    }

    public String content() {
        return content;
    }
}

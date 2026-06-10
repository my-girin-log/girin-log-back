package com.girinlog.memo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "memos")
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    protected Memo() {
    }

    private Memo(Long userId, LocalDate serviceDate, String content, OffsetDateTime createdAt) {
        this.userId = userId;
        this.serviceDate = serviceDate;
        this.content = content;
        this.status = MemoStatus.DRAFT;
        this.createdAt = createdAt;
    }

    public static Memo draft(Long userId, LocalDate serviceDate, String content, OffsetDateTime createdAt) {
        return new Memo(userId, serviceDate, content, createdAt);
    }

    public void updateContent(String content, OffsetDateTime updatedAt) {
        if (!isDraft()) {
            throw new IllegalStateException("DRAFT 상태의 Memo만 수정할 수 있습니다.");
        }
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public void summarize() {
        if (!isDraft()) {
            throw new IllegalStateException("DRAFT 상태의 Memo만 요약할 수 있습니다.");
        }
        this.status = MemoStatus.SUMMARIZED;
    }

    /** 06:00 KST 일일 작업 공간 초기화 시 DRAFT Memo를 작업 공간에서 제외한다. */
    public void archive() {
        if (!isDraft()) {
            throw new IllegalStateException("DRAFT 상태의 Memo만 ARCHIVED로 전환할 수 있습니다.");
        }
        this.status = MemoStatus.ARCHIVED;
    }

    public boolean isDraft() {
        return status == MemoStatus.DRAFT;
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

    public String content() {
        return content;
    }

    public MemoStatus status() {
        return status;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime updatedAt() {
        return updatedAt;
    }
}

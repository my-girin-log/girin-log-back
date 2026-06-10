package com.girinlog.persona.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persona 생성을 위한 원천 입력(블로그 링크 / 기존 글 원문 / 설문). (data-model PersonaSource)
 */
@Entity
@Table(name = "persona_sources")
public class PersonaSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    private AnalysisStatus analysisStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PersonaSource() {
    }

    private PersonaSource(Long userId, SourceType sourceType, String content, AnalysisStatus analysisStatus) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.content = content;
        this.analysisStatus = analysisStatus;
    }

    /** 블로그 링크. 분석 대상이므로 PENDING으로 시작한다. */
    public static PersonaSource blogUrl(Long userId, String url) {
        return new PersonaSource(userId, SourceType.BLOG_URL, url, AnalysisStatus.PENDING);
    }

    /** 기존 글 원문. 분석 불필요(COMPLETED). */
    public static PersonaSource text(Long userId, String rawText) {
        return new PersonaSource(userId, SourceType.TEXT, rawText, AnalysisStatus.COMPLETED);
    }

    /** 설문 응답. 분석 불필요(COMPLETED). */
    public static PersonaSource survey(Long userId, String answersJson) {
        return new PersonaSource(userId, SourceType.SURVEY, answersJson, AnalysisStatus.COMPLETED);
    }

    public void markAnalyzing() {
        this.analysisStatus = AnalysisStatus.ANALYZING;
    }

    public void markCompleted() {
        this.analysisStatus = AnalysisStatus.COMPLETED;
    }

    /** 분석 실패해도 설문 기반 Persona 생성은 계속된다(data-model PersonaSource). */
    public void markFailed() {
        this.analysisStatus = AnalysisStatus.FAILED;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long id() {
        return id;
    }

    public SourceType sourceType() {
        return sourceType;
    }

    public AnalysisStatus analysisStatus() {
        return analysisStatus;
    }
}

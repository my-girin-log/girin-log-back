package com.girinlog.persona.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 온보딩 설문 응답. 응답 원문은 JSON 문자열로 보관한다(data-model OnboardingSurvey).
 */
@Entity
@Table(name = "onboarding_surveys")
public class OnboardingSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "answers_json", columnDefinition = "TEXT", nullable = false)
    private String answersJson;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected OnboardingSurvey() {
    }

    private OnboardingSurvey(Long userId, String answersJson) {
        this.userId = userId;
        this.answersJson = answersJson;
    }

    public static OnboardingSurvey of(Long userId, String answersJson) {
        return new OnboardingSurvey(userId, answersJson);
    }

    @PrePersist
    void onCreate() {
        this.submittedAt = Instant.now();
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String answersJson() {
        return answersJson;
    }

    public Instant submittedAt() {
        return submittedAt;
    }
}

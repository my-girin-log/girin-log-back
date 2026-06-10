package com.girinlog.persona.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.girinlog.persona.generation.GeneratedPersona;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자의 말투·사고 흐름·회고 기준을 요약한 Persona. (data-model Persona)
 * 사용자당 1개. 사용자 기록이 쌓이면 {@link #refresh}로 주기적 갱신한다(매일 고정 아님).
 */
@Entity
@Table(
        name = "personas",
        uniqueConstraints = @UniqueConstraint(name = "uk_persona_user_id", columnNames = "user_id"))
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "tone", nullable = false)
    private String tone;

    @Column(name = "thinking_style", nullable = false)
    private String thinkingStyle;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "persona_recurring_interests", joinColumns = @JoinColumn(name = "persona_id"))
    @Column(name = "interest")
    private List<String> recurringInterests = new ArrayList<>();

    @Column(name = "organizing_habit")
    private String organizingHabit;

    @Column(name = "retrospection_criteria")
    private String retrospectionCriteria;

    @Column(name = "preferred_structure")
    private String preferredStructure;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "markdown", columnDefinition = "TEXT")
    private String markdown;

    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Persona() {
    }

    private Persona(Long userId, GeneratedPersona generated) {
        this.userId = userId;
        applyGenerated(generated);
    }

    public static Persona create(Long userId, GeneratedPersona generated) {
        return new Persona(userId, generated);
    }

    /** 사용자 기록 기반 재생성 결과로 Persona를 갱신한다. */
    public void refresh(GeneratedPersona generated) {
        applyGenerated(generated);
        this.lastRefreshedAt = Instant.now();
    }

    private void applyGenerated(GeneratedPersona generated) {
        this.tone = generated.tone();
        this.thinkingStyle = generated.thinkingStyle();
        this.recurringInterests = new ArrayList<>(generated.recurringInterests());
        this.organizingHabit = generated.organizingHabit();
        this.retrospectionCriteria = generated.retrospectionCriteria();
        this.preferredStructure = generated.preferredStructure();
        this.summary = generated.summary();
        this.markdown = generated.markdown();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String tone() {
        return tone;
    }

    public String thinkingStyle() {
        return thinkingStyle;
    }

    public List<String> recurringInterests() {
        return List.copyOf(recurringInterests);
    }

    public String organizingHabit() {
        return organizingHabit;
    }

    public String retrospectionCriteria() {
        return retrospectionCriteria;
    }

    public String preferredStructure() {
        return preferredStructure;
    }

    public String summary() {
        return summary;
    }

    public String markdown() {
        return markdown;
    }

    public Instant lastRefreshedAt() {
        return lastRefreshedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}

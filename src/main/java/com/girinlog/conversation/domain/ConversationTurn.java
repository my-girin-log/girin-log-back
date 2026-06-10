package com.girinlog.conversation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.OffsetDateTime;
import java.util.Objects;

@Embeddable
public class ConversationTurn {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected ConversationTurn() {
    }

    private ConversationTurn(ConversationRole role, String content, OffsetDateTime createdAt) {
        this.role = Objects.requireNonNull(role, "role은 필수입니다.");
        this.content = requireContent(content);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수입니다.");
    }

    public static ConversationTurn silok(String content, OffsetDateTime createdAt) {
        return new ConversationTurn(ConversationRole.SILOK, content, createdAt);
    }

    public static ConversationTurn user(String content, OffsetDateTime createdAt) {
        return new ConversationTurn(ConversationRole.USER, content, createdAt);
    }

    public ConversationRole role() {
        return role;
    }

    public String content() {
        return content;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    private static String requireContent(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("대화 내용은 비어 있을 수 없습니다.");
        }
        return value;
    }
}

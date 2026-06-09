package com.girinlog.conversation.controller;

import com.girinlog.conversation.domain.ConversationRole;
import com.girinlog.conversation.domain.ConversationTurn;

import java.time.OffsetDateTime;

public record ConversationTurnResponse(
        ConversationRole role,
        String content,
        OffsetDateTime createdAt
) {

    static ConversationTurnResponse from(ConversationTurn turn) {
        return new ConversationTurnResponse(turn.role(), turn.content(), turn.createdAt());
    }
}

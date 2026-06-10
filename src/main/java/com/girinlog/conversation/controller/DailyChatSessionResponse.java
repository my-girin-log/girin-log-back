package com.girinlog.conversation.controller;

import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.DailyChatSessionStatus;
import com.girinlog.conversation.domain.EndedReason;

import java.time.OffsetDateTime;
import java.util.List;

public record DailyChatSessionResponse(
        Long sessionId,
        DailyChatSessionStatus status,
        int followUpCount,
        int maxFollowUpCount,
        List<Long> memoSummaryIds,
        List<ConversationTurnResponse> conversation,
        EndedReason endedReason,
        String closingMessage,
        OffsetDateTime createdAt,
        OffsetDateTime endedAt
) {

    static DailyChatSessionResponse from(DailyChatSession session) {
        return new DailyChatSessionResponse(
                session.id(),
                session.status(),
                session.followUpCount(),
                session.maxFollowUpCount(),
                session.memoSummaryIds(),
                session.conversation().stream()
                        .map(ConversationTurnResponse::from)
                        .toList(),
                session.endedReason(),
                session.closingMessage(),
                session.createdAt(),
                session.endedAt()
        );
    }
}

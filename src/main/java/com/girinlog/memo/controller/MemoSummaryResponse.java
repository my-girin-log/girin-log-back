package com.girinlog.memo.controller;

import com.girinlog.memo.domain.MemoSummary;
import com.girinlog.memo.domain.MemoSummaryChatDisabledReason;

import java.time.OffsetDateTime;
import java.util.List;

public record MemoSummaryResponse(
        Long id,
        String categoryName,
        String summary,
        int itemCount,
        List<MemoSummaryItemResponse> items,
        boolean chatAvailable,
        MemoSummaryChatDisabledReason chatDisabledReason,
        OffsetDateTime createdAt
) {

    public static MemoSummaryResponse from(MemoSummary memoSummary) {
        return new MemoSummaryResponse(
                memoSummary.id(),
                memoSummary.categoryName(),
                memoSummary.summary(),
                memoSummary.itemCount(),
                memoSummary.items().stream()
                        .map(MemoSummaryItemResponse::from)
                        .toList(),
                memoSummary.chatAvailable(),
                memoSummary.chatDisabledReason(),
                memoSummary.createdAt()
        );
    }
}

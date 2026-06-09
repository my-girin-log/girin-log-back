package com.girinlog.memo.controller;

import com.girinlog.memo.domain.MemoSummaryItem;

public record MemoSummaryItemResponse(
        Long id,
        String content
) {

    public static MemoSummaryItemResponse from(MemoSummaryItem item) {
        return new MemoSummaryItemResponse(item.id(), item.content());
    }
}

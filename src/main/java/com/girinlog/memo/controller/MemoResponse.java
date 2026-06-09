package com.girinlog.memo.controller;

import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.domain.MemoStatus;

import java.time.OffsetDateTime;

public record MemoResponse(
        Long id,
        String content,
        MemoStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.id(),
                memo.content(),
                memo.status(),
                memo.createdAt(),
                memo.updatedAt()
        );
    }
}

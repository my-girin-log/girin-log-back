package com.girinlog.memo.controller;

import com.girinlog.memo.domain.Memo;

import java.time.LocalDate;
import java.util.List;

public record MemoListResponse(
        LocalDate date,
        List<MemoResponse> memos
) {

    public static MemoListResponse of(LocalDate date, List<Memo> memos) {
        return new MemoListResponse(date, memos.stream()
                .map(MemoResponse::from)
                .toList());
    }
}

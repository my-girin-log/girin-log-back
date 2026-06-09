package com.girinlog.memo.controller;

import com.girinlog.memo.domain.MemoSummary;

import java.time.LocalDate;
import java.util.List;

public record MemoSummaryListResponse(
        LocalDate date,
        List<MemoSummaryResponse> memoSummaries
) {

    public static MemoSummaryListResponse of(LocalDate date, List<MemoSummary> memoSummaries) {
        return new MemoSummaryListResponse(date, memoSummaries.stream()
                .map(MemoSummaryResponse::from)
                .toList());
    }
}

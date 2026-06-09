package com.girinlog.memo.controller;

import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.domain.MemoSummary;

import java.time.LocalDate;
import java.util.List;

public record MemoSummaryResultResponse(
        LocalDate date,
        List<MemoSummaryResponse> memoSummaries,
        MemoResponse nextMemo
) {

    public static MemoSummaryResultResponse of(LocalDate date, List<MemoSummary> memoSummaries, Memo nextMemo) {
        return new MemoSummaryResultResponse(
                date,
                memoSummaries.stream()
                        .map(MemoSummaryResponse::from)
                        .toList(),
                MemoResponse.from(nextMemo)
        );
    }
}

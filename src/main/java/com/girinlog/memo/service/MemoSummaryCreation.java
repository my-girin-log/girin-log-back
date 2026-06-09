package com.girinlog.memo.service;

import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.domain.MemoSummary;

import java.time.LocalDate;
import java.util.List;

public record MemoSummaryCreation(
        LocalDate date,
        List<MemoSummary> memoSummaries,
        Memo nextMemo
) {
}

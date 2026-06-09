package com.girinlog.memo.service;

import java.util.List;

public record MemoSummaryCandidate(
        String categoryName,
        String summary,
        List<MemoSummaryItemCandidate> items
) {
}

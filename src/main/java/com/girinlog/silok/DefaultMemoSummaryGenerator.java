package com.girinlog.silok;

import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.service.MemoSummaryCandidate;
import com.girinlog.memo.service.MemoSummaryGenerator;
import com.girinlog.memo.service.MemoSummaryItemCandidate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class DefaultMemoSummaryGenerator implements MemoSummaryGenerator {

    private static final int SUMMARY_PREVIEW_LENGTH = 200;

    @Override
    public List<MemoSummaryCandidate> generate(List<Memo> memos) {
        List<MemoSummaryItemCandidate> items = memos.stream()
                .map(memo -> new MemoSummaryItemCandidate(memo.id(), memo.content()))
                .toList();
        return List.of(new MemoSummaryCandidate("기록", summarize(memos), items));
    }

    private String summarize(List<Memo> memos) {
        String combined = memos.stream()
                .map(Memo::content)
                .map(String::trim)
                .filter(content -> !content.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        if (combined.length() <= SUMMARY_PREVIEW_LENGTH) {
            return combined;
        }
        return combined.substring(0, SUMMARY_PREVIEW_LENGTH);
    }
}

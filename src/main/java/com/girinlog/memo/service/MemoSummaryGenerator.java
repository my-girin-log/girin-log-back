package com.girinlog.memo.service;

import com.girinlog.memo.domain.Memo;

import java.util.List;

public interface MemoSummaryGenerator {

    List<MemoSummaryCandidate> generate(List<Memo> memos);
}

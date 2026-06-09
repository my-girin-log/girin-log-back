package com.girinlog.memo.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.time.ServiceDay;
import com.girinlog.memo.MemoErrorCode;
import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.domain.MemoStatus;
import com.girinlog.memo.domain.MemoSummary;
import com.girinlog.memo.domain.MemoSummaryItem;
import com.girinlog.memo.repository.MemoRepository;
import com.girinlog.memo.repository.MemoSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class MemoService {

    private final MemoRepository memoRepository;
    private final MemoSummaryRepository memoSummaryRepository;
    private final MemoSummaryGenerator memoSummaryGenerator;
    private final Clock clock;

    public MemoService(
            MemoRepository memoRepository,
            MemoSummaryRepository memoSummaryRepository,
            MemoSummaryGenerator memoSummaryGenerator,
            Clock clock
    ) {
        this.memoRepository = memoRepository;
        this.memoSummaryRepository = memoSummaryRepository;
        this.memoSummaryGenerator = memoSummaryGenerator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Memo> listMemos(Long userId, LocalDate requestedDate) {
        LocalDate date = defaultToday(requestedDate);
        return memoRepository.findByUserIdAndServiceDateOrderByCreatedAtAsc(userId, date);
    }

    @Transactional
    public Memo createMemo(Long userId, String content) {
        Memo memo = Memo.draft(userId, ServiceDay.today(clock), content, now());
        return memoRepository.save(memo);
    }

    @Transactional
    public MemoSummaryCreation createMemoSummaries(Long userId, LocalDate requestedDate) {
        LocalDate date = defaultToday(requestedDate);
        List<Memo> sourceMemos = memoRepository.findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(
                userId,
                date,
                MemoStatus.DRAFT
        );
        if (sourceMemos.isEmpty()) {
            throw new BusinessException(MemoErrorCode.NO_SUMMARIZABLE_MEMO);
        }

        OffsetDateTime createdAt = now();
        List<MemoSummary> memoSummaries = memoSummaryGenerator.generate(sourceMemos).stream()
                .map(candidate -> toMemoSummary(userId, date, candidate, createdAt))
                .toList();
        sourceMemos.forEach(Memo::summarize);
        Memo nextMemo = Memo.draft(userId, date, "", createdAt);

        List<MemoSummary> savedMemoSummaries = memoSummaryRepository.saveAll(memoSummaries);
        Memo savedNextMemo = memoRepository.save(nextMemo);
        return new MemoSummaryCreation(date, savedMemoSummaries, savedNextMemo);
    }

    @Transactional(readOnly = true)
    public List<MemoSummary> listMemoSummaries(Long userId, LocalDate requestedDate) {
        LocalDate date = defaultToday(requestedDate);
        return memoSummaryRepository.findByUserIdAndServiceDateOrderByCreatedAtAsc(userId, date);
    }

    public LocalDate defaultToday(LocalDate requestedDate) {
        if (requestedDate != null) {
            return requestedDate;
        }
        return ServiceDay.today(clock);
    }

    private MemoSummary toMemoSummary(
            Long userId,
            LocalDate date,
            MemoSummaryCandidate candidate,
            OffsetDateTime createdAt
    ) {
        List<MemoSummaryItem> items = candidate.items().stream()
                .map(item -> new MemoSummaryItem(item.memoId(), item.content()))
                .toList();
        return MemoSummary.create(userId, date, candidate.categoryName(), candidate.summary(), items, createdAt);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}

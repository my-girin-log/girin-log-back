package com.girinlog.diary.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.DailyChatSessionStatus;
import com.girinlog.conversation.repository.DailyChatSessionRepository;
import com.girinlog.diary.DiaryErrorCode;
import com.girinlog.diary.domain.Diary;
import com.girinlog.diary.repository.DiaryRepository;
import com.girinlog.event.domain.EventType;
import com.girinlog.event.service.EventLogRecorder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DiaryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final DiaryRepository diaryRepository;
    private final DailyChatSessionRepository dailyChatSessionRepository;
    private final DiaryGenerator diaryGenerator;
    private final EventLogRecorder eventLogRecorder;
    private final Clock clock;

    public DiaryService(
            DiaryRepository diaryRepository,
            DailyChatSessionRepository dailyChatSessionRepository,
            DiaryGenerator diaryGenerator,
            EventLogRecorder eventLogRecorder,
            Clock clock
    ) {
        this.diaryRepository = diaryRepository;
        this.dailyChatSessionRepository = dailyChatSessionRepository;
        this.diaryGenerator = diaryGenerator;
        this.eventLogRecorder = eventLogRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DiaryPage listDiaries(Long userId, LocalDate startDate, LocalDate endDate, String cursor, Integer limit) {
        validateDateRange(startDate, endDate);
        int pageSize = normalizeLimit(limit);
        List<Diary> diaries = diaryRepository.findPageByDateRange(
                userId,
                startDate,
                endDate,
                DiaryCursor.decode(cursor),
                PageRequest.of(0, pageSize + 1)
        );

        if (diaries.size() <= pageSize) {
            return new DiaryPage(diaries, null);
        }

        List<Diary> items = diaries.subList(0, pageSize);
        String nextCursor = DiaryCursor.encode(items.getLast().serviceDate());
        return new DiaryPage(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> listDiaryDates(Long userId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return diaryRepository.findServiceDatesByDateRange(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Diary getDiaryByDate(Long userId, LocalDate date) {
        return diaryRepository.findByUserIdAndServiceDate(userId, date)
                .orElseThrow(() -> new BusinessException(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    @Transactional
    public Optional<Diary> generateDailyDiary(Long userId, LocalDate serviceDate) {
        Optional<Diary> existingDiary = diaryRepository.findByUserIdAndServiceDate(userId, serviceDate);
        if (existingDiary.isPresent()) {
            return existingDiary;
        }

        List<DailyChatSession> sessions = dailyChatSessionRepository
                .findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(
                        userId,
                        serviceDate,
                        DailyChatSessionStatus.ENDED
                );
        if (sessions.isEmpty()) {
            return Optional.empty();
        }

        DiaryContent content = diaryGenerator.generate(serviceDate, sessions);
        Diary diary = Diary.create(
                userId,
                serviceDate,
                content.summary(),
                content.mainEvents(),
                content.emotionContext(),
                content.concerns(),
                content.newCriteria(),
                content.nextActions(),
                content.markdown(),
                OffsetDateTime.now(clock)
        );
        Diary savedDiary = diaryRepository.save(diary);
        eventLogRecorder.record(userId, EventType.DIARY_CREATED, metadata()
                .add("diaryId", savedDiary.id())
                .add("serviceDate", savedDiary.serviceDate().toString())
                .add("dailyChatSessionIds", sessions.stream().map(DailyChatSession::id).toList())
                .toMap());
        return Optional.of(savedDiary);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(DiaryErrorCode.INVALID_DIARY_DATE_RANGE);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(DiaryErrorCode.INVALID_DIARY_DATE_RANGE, "limit은 1 이상 100 이하여야 합니다.");
        }
        return limit;
    }

    private Metadata metadata() {
        return new Metadata();
    }

    private static class Metadata {

        private final Map<String, Object> values = new LinkedHashMap<>();

        Metadata add(String key, Object value) {
            values.put(key, value);
            return this;
        }

        Map<String, Object> toMap() {
            return values;
        }
    }
}

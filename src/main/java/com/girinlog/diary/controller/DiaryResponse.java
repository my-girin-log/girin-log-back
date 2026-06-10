package com.girinlog.diary.controller;

import com.girinlog.diary.domain.Diary;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DiaryResponse(
        Long id,
        LocalDate date,
        String summary,
        List<String> mainEvents,
        String emotionContext,
        String concerns,
        String newCriteria,
        String nextActions,
        String markdown,
        OffsetDateTime createdAt
) {

    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
                diary.id(),
                diary.serviceDate(),
                diary.summary(),
                diary.mainEvents(),
                diary.emotionContext(),
                diary.concerns(),
                diary.newCriteria(),
                diary.nextActions(),
                diary.markdown(),
                diary.createdAt()
        );
    }
}

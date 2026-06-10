package com.girinlog.diary.service;

import com.girinlog.diary.domain.Diary;

import java.util.List;

public record DiaryPage(
        List<Diary> items,
        String nextCursor
) {
}

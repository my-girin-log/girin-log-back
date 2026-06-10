package com.girinlog.diary.controller;

import com.girinlog.diary.service.DiaryPage;

import java.util.List;

public record DiaryListResponse(
        List<DiaryResponse> items,
        String nextCursor
) {

    public static DiaryListResponse from(DiaryPage page) {
        return new DiaryListResponse(
                page.items().stream()
                        .map(DiaryResponse::from)
                        .toList(),
                page.nextCursor()
        );
    }
}

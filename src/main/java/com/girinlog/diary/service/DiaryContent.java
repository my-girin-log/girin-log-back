package com.girinlog.diary.service;

import java.util.List;

public record DiaryContent(
        String summary,
        List<String> mainEvents,
        String emotionContext,
        String concerns,
        String newCriteria,
        String nextActions,
        String markdown
) {
}

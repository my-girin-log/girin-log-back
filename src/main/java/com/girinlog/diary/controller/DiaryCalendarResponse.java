package com.girinlog.diary.controller;

import java.time.LocalDate;
import java.util.List;

public record DiaryCalendarResponse(
        List<LocalDate> dates
) {
}

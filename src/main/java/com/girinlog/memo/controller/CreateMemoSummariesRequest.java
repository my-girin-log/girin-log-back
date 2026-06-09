package com.girinlog.memo.controller;

import java.time.LocalDate;

public record CreateMemoSummariesRequest(
        LocalDate date
) {
}

package com.girinlog.diary.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.diary.DiaryErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;

final class DiaryCursor {

    private DiaryCursor() {
    }

    static String encode(LocalDate serviceDate) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(serviceDate.toString().getBytes(StandardCharsets.UTF_8));
    }

    static LocalDate decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return LocalDate.parse(decoded);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BusinessException(DiaryErrorCode.INVALID_DIARY_CURSOR);
        }
    }
}

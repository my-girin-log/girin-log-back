package com.girinlog.retrospective.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.retrospective.RetrospectiveErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class RetrospectiveCursor {

    private RetrospectiveCursor() {
    }

    static String encode(Long retrospectiveId) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(String.valueOf(retrospectiveId).getBytes(StandardCharsets.UTF_8));
    }

    static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Long.valueOf(decoded);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(RetrospectiveErrorCode.INVALID_RETROSPECTIVE_CURSOR);
        }
    }
}

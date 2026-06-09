package com.girinlog.auth.controller.dto;

import com.girinlog.auth.domain.User;
import com.girinlog.common.time.ServiceClockConfig;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * 사용자 정보 응답. (openapi User 스키마)
 * 시각은 KST 오프셋(+09:00)으로 내려준다(conventions/api.md 1절).
 */
public record UserResponse(
        Long id,
        String githubId,
        String githubUsername,
        String profileImageUrl,
        String nickname,
        boolean onboardingCompleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(),
                user.githubId(),
                user.githubUsername(),
                user.profileImageUrl(),
                user.nickname(),
                user.isOnboardingCompleted(),
                toKst(user.createdAt()),
                toKst(user.updatedAt()));
    }

    private static OffsetDateTime toKst(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ServiceClockConfig.KST).toOffsetDateTime();
    }
}

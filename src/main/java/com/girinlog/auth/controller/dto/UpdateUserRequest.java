package com.girinlog.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 내 정보 수정 요청. (openapi UpdateUserRequest)
 */
public record UpdateUserRequest(
        @NotBlank
        @Size(min = 1, max = 30)
        String nickname) {
}

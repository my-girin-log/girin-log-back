package com.girinlog.memo.controller;

import jakarta.validation.constraints.NotBlank;

public record CreateMemoRequest(
        @NotBlank String content
) {
}

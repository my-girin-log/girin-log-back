package com.girinlog.conversation.controller;

import jakarta.validation.constraints.NotBlank;

public record SubmitChatAnswerRequest(
        @NotBlank
        String content
) {
}

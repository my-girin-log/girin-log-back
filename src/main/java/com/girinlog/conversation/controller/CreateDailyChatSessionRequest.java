package com.girinlog.conversation.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateDailyChatSessionRequest(
        @NotEmpty
        List<@NotNull Long> memoSummaryIds
) {
}

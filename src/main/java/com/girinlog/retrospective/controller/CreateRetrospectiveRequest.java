package com.girinlog.retrospective.controller;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateRetrospectiveRequest(
        @NotNull(message = "startDate는 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "endDate는 필수입니다.")
        LocalDate endDate
) {
}

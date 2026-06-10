package com.girinlog.retrospective.controller;

import com.girinlog.retrospective.service.RetrospectivePage;

import java.util.List;

public record RetrospectiveListResponse(
        List<RetrospectiveResponse> items,
        String nextCursor
) {

    public static RetrospectiveListResponse from(RetrospectivePage page) {
        return new RetrospectiveListResponse(
                page.items().stream()
                        .map(RetrospectiveResponse::from)
                        .toList(),
                page.nextCursor()
        );
    }
}

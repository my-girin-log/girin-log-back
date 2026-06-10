package com.girinlog.retrospective.service;

import com.girinlog.retrospective.domain.Retrospective;

import java.util.List;

public record RetrospectivePage(
        List<Retrospective> items,
        String nextCursor
) {
}

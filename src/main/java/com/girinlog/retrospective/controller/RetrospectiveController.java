package com.girinlog.retrospective.controller;

import com.girinlog.retrospective.service.RetrospectiveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retrospectives")
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    public RetrospectiveController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    @GetMapping
    public RetrospectiveListResponse listRetrospectives(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return RetrospectiveListResponse.from(
                retrospectiveService.listRetrospectives(currentUserId(jwt), cursor, limit)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RetrospectiveResponse createRetrospective(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateRetrospectiveRequest request
    ) {
        return RetrospectiveResponse.from(retrospectiveService.createRetrospective(
                currentUserId(jwt),
                request.startDate(),
                request.endDate()
        ));
    }

    @GetMapping("/{retrospectiveId}")
    public RetrospectiveResponse getRetrospective(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long retrospectiveId
    ) {
        return RetrospectiveResponse.from(
                retrospectiveService.getRetrospective(currentUserId(jwt), retrospectiveId)
        );
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

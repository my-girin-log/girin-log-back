package com.girinlog.conversation.controller;

import com.girinlog.conversation.service.DailyChatSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-chat-sessions")
public class DailyChatSessionController {

    private final DailyChatSessionService dailyChatSessionService;

    public DailyChatSessionController(DailyChatSessionService dailyChatSessionService) {
        this.dailyChatSessionService = dailyChatSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DailyChatSessionResponse createDailyChatSession(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateDailyChatSessionRequest request
    ) {
        return DailyChatSessionResponse.from(
                dailyChatSessionService.createDailyChatSession(currentUserId(jwt), request.memoSummaryIds())
        );
    }

    @GetMapping("/{sessionId}")
    public DailyChatSessionResponse getDailyChatSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId
    ) {
        return DailyChatSessionResponse.from(
                dailyChatSessionService.getDailyChatSession(currentUserId(jwt), sessionId)
        );
    }

    @PostMapping("/{sessionId}/answers")
    public DailyChatSessionResponse submitDailyChatAnswer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId,
            @RequestBody @Valid SubmitChatAnswerRequest request
    ) {
        return DailyChatSessionResponse.from(
                dailyChatSessionService.submitAnswer(currentUserId(jwt), sessionId, request.content())
        );
    }

    @PostMapping("/{sessionId}/end")
    public DailyChatSessionResponse endDailyChatSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId
    ) {
        return DailyChatSessionResponse.from(
                dailyChatSessionService.endSession(currentUserId(jwt), sessionId)
        );
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

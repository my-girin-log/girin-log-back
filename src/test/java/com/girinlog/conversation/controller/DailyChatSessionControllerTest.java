package com.girinlog.conversation.controller;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.error.GlobalExceptionHandler;
import com.girinlog.conversation.ConversationErrorCode;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.conversation.service.DailyChatSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DailyChatSessionController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, DailyChatSessionControllerTest.JwtArgumentResolverConfig.class})
class DailyChatSessionControllerTest {

    private static final Long USER_ID = 1L;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T09:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DailyChatSessionService dailyChatSessionService;

    @Test
    @DisplayName("DailyChatSession을 생성하면 201과 첫 질문이 포함된 세션을 반환한다")
    void create_daily_chat_session_returns_created_session() throws Exception {
        DailyChatSession session = session(100L);
        given(dailyChatSessionService.createDailyChatSession(USER_ID, List.of(10L))).willReturn(session);

        mockMvc.perform(post("/api/daily-chat-sessions")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"memoSummaryIds":[10]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(100))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.followUpCount").value(1))
                .andExpect(jsonPath("$.maxFollowUpCount").value(10))
                .andExpect(jsonPath("$.memoSummaryIds[0]").value(10))
                .andExpect(jsonPath("$.conversation[0].role").value("SILOK"))
                .andExpect(jsonPath("$.conversation[0].content").value("첫 질문"))
                .andExpect(jsonPath("$.endedReason").value(nullValue()))
                .andExpect(jsonPath("$.closingMessage").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").value("2026-06-08T09:00:00+09:00"))
                .andExpect(jsonPath("$.endedAt").value(nullValue()));
    }

    @Test
    @DisplayName("memoSummaryIds가 비어 있으면 400 에러 envelope를 반환한다")
    void create_daily_chat_session_requires_memo_summary_ids() throws Exception {
        mockMvc.perform(post("/api/daily-chat-sessions")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"memoSummaryIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.memoSummaryIds").exists());
    }

    @Test
    @DisplayName("이미 대화에 사용된 MemoSummary 선택은 422를 반환한다")
    void create_daily_chat_session_with_unavailable_memo_summary_returns_422() throws Exception {
        given(dailyChatSessionService.createDailyChatSession(USER_ID, List.of(10L)))
                .willThrow(new BusinessException(ConversationErrorCode.MEMO_SUMMARY_NOT_CHAT_AVAILABLE));

        mockMvc.perform(post("/api/daily-chat-sessions")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"memoSummaryIds":[10]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("MEMO_SUMMARY_NOT_CHAT_AVAILABLE"));
    }

    @Test
    @DisplayName("DailyChatSession 조회는 conversation을 포함한다")
    void get_daily_chat_session_returns_session() throws Exception {
        DailyChatSession session = session(100L);
        given(dailyChatSessionService.getDailyChatSession(USER_ID, 100L)).willReturn(session);

        mockMvc.perform(get("/api/daily-chat-sessions/100")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(100))
                .andExpect(jsonPath("$.conversation[0].content").value("첫 질문"));
    }

    @Test
    @DisplayName("답변 저장은 사용자 답변과 다음 질문이 반영된 세션을 반환한다")
    void submit_daily_chat_answer_returns_updated_session() throws Exception {
        DailyChatSession session = session(100L);
        session.addUserAnswer("조금 막막했어.", CREATED_AT);
        session.addSilokFollowUpQuestion("어떤 부분이 제일 막막했어?", CREATED_AT);
        given(dailyChatSessionService.submitAnswer(USER_ID, 100L, "조금 막막했어.")).willReturn(session);

        mockMvc.perform(post("/api/daily-chat-sessions/100/answers")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"content":"조금 막막했어."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation[1].role").value("USER"))
                .andExpect(jsonPath("$.conversation[1].content").value("조금 막막했어."))
                .andExpect(jsonPath("$.conversation[2].role").value("SILOK"))
                .andExpect(jsonPath("$.followUpCount").value(2));
    }

    @Test
    @DisplayName("빈 답변은 400 에러 envelope를 반환한다")
    void submit_daily_chat_answer_requires_content() throws Exception {
        mockMvc.perform(post("/api/daily-chat-sessions/100/answers")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.content").exists());
    }

    @Test
    @DisplayName("세션 종료는 마무리 멘트를 포함한 ENDED 세션을 반환한다")
    void end_daily_chat_session_returns_ended_session() throws Exception {
        DailyChatSession session = session(100L);
        session.end(EndedReason.USER_ENDED, "오늘 얘기는 여기까지 정리해둘게.", CREATED_AT);
        given(dailyChatSessionService.endSession(USER_ID, 100L)).willReturn(session);

        mockMvc.perform(post("/api/daily-chat-sessions/100/end")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.endedReason").value("USER_ENDED"))
                .andExpect(jsonPath("$.closingMessage").value("오늘 얘기는 여기까지 정리해둘게."))
                .andExpect(jsonPath("$.conversation[1].content").value("오늘 얘기는 여기까지 정리해둘게."));
    }

    private DailyChatSession session(Long id) {
        DailyChatSession session = DailyChatSession.start(
                USER_ID,
                LocalDate.of(2026, 6, 8),
                List.of(10L),
                "snapshot",
                "첫 질문",
                CREATED_AT
        );
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    @TestConfiguration
    static class JwtArgumentResolverConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new FixedJwtArgumentResolver());
        }
    }

    static class FixedJwtArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(Jwt.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory
        ) {
            return Jwt.withTokenValue("test-token")
                    .header("alg", "none")
                    .subject(String.valueOf(USER_ID))
                    .build();
        }
    }
}

package com.girinlog.memo.controller;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.error.GlobalExceptionHandler;
import com.girinlog.memo.MemoErrorCode;
import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.domain.MemoSummary;
import com.girinlog.memo.domain.MemoSummaryItem;
import com.girinlog.memo.service.MemoService;
import com.girinlog.memo.service.MemoSummaryCreation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MemoController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MemoControllerTest.JwtArgumentResolverConfig.class})
class MemoControllerTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 8);
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T09:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemoService memoService;

    @Test
    @DisplayName("Memo를 생성하면 201과 생성된 Memo를 반환한다")
    void create_memo_returns_created_memo() throws Exception {
        Memo memo = memo(1L, "오늘 배운 내용");
        given(memoService.createMemo(USER_ID, "오늘 배운 내용")).willReturn(memo);

        mockMvc.perform(post("/api/memos")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"content":"오늘 배운 내용"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("오늘 배운 내용"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").value("2026-06-08T09:00:00+09:00"))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));
    }

    @Test
    @DisplayName("Memo 내용이 비어 있으면 400 에러 envelope를 반환한다")
    void create_memo_with_blank_content_returns_error_envelope() throws Exception {
        mockMvc.perform(post("/api/memos")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("형식 또는 필수값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.error.details.content").exists());
    }

    @Test
    @DisplayName("Memo 목록은 서비스 날짜와 Memo 배열을 반환한다")
    void list_memos_returns_date_and_memos() throws Exception {
        Memo memo = memo(1L, "오늘 배운 내용");
        given(memoService.defaultToday(SERVICE_DATE)).willReturn(SERVICE_DATE);
        given(memoService.listMemos(USER_ID, SERVICE_DATE)).willReturn(List.of(memo));

        mockMvc.perform(get("/api/memos")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .param("date", "2026-06-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-06-08"))
                .andExpect(jsonPath("$.memos[0].id").value(1))
                .andExpect(jsonPath("$.memos[0].content").value("오늘 배운 내용"))
                .andExpect(jsonPath("$.memos[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("MemoSummary 생성은 요약 목록과 다음 DRAFT Memo를 반환한다")
    void create_memo_summaries_returns_summaries_and_next_memo() throws Exception {
        Memo nextMemo = memo(2L, "");
        MemoSummary memoSummary = memoSummary(10L, 100L);
        given(memoService.createMemoSummaries(eq(USER_ID), isNull()))
                .willReturn(new MemoSummaryCreation(SERVICE_DATE, List.of(memoSummary), nextMemo));

        mockMvc.perform(post("/api/memos/summaries")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value("2026-06-08"))
                .andExpect(jsonPath("$.memoSummaries[0].id").value(10))
                .andExpect(jsonPath("$.memoSummaries[0].categoryName").value("학습"))
                .andExpect(jsonPath("$.memoSummaries[0].summary").value("오늘 배운 내용"))
                .andExpect(jsonPath("$.memoSummaries[0].itemCount").value(1))
                .andExpect(jsonPath("$.memoSummaries[0].chatAvailable").value(true))
                .andExpect(jsonPath("$.memoSummaries[0].chatDisabledReason").value(nullValue()))
                .andExpect(jsonPath("$.memoSummaries[0].items[0].id").value(100))
                .andExpect(jsonPath("$.memoSummaries[0].items[0].content").value("오늘 배운 내용"))
                .andExpect(jsonPath("$.memoSummaries[0].items[0].memoId").doesNotExist())
                .andExpect(jsonPath("$.nextMemo.id").value(2))
                .andExpect(jsonPath("$.nextMemo.content").value(""))
                .andExpect(jsonPath("$.nextMemo.status").value("DRAFT"));
    }

    @Test
    @DisplayName("요약할 DRAFT Memo가 없으면 422와 NO_SUMMARIZABLE_MEMO를 반환한다")
    void create_memo_summaries_without_draft_memo_returns_422() throws Exception {
        given(memoService.createMemoSummaries(eq(USER_ID), isNull()))
                .willThrow(new BusinessException(MemoErrorCode.NO_SUMMARIZABLE_MEMO));

        mockMvc.perform(post("/api/memos/summaries")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_SUMMARIZABLE_MEMO"))
                .andExpect(jsonPath("$.error.details").value(nullValue()));
    }

    @Test
    @DisplayName("MemoSummary 목록은 대화 가능 여부를 포함하고 item의 memoId를 숨긴다")
    void list_memo_summaries_returns_chat_availability_and_items_without_memo_id() throws Exception {
        MemoSummary memoSummary = memoSummary(10L, 100L);
        given(memoService.defaultToday(SERVICE_DATE)).willReturn(SERVICE_DATE);
        given(memoService.listMemoSummaries(USER_ID, SERVICE_DATE)).willReturn(List.of(memoSummary));

        mockMvc.perform(get("/api/memo-summaries")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .param("date", "2026-06-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-06-08"))
                .andExpect(jsonPath("$.memoSummaries[0].chatAvailable").value(true))
                .andExpect(jsonPath("$.memoSummaries[0].chatDisabledReason").value(nullValue()))
                .andExpect(jsonPath("$.memoSummaries[0].items[0].content").value("오늘 배운 내용"))
                .andExpect(jsonPath("$.memoSummaries[0].items[0].memoId").doesNotExist());
    }

    @Test
    @DisplayName("이미 대화에 사용된 MemoSummary는 비활성화 사유와 함께 반환한다")
    void list_memo_summaries_returns_disabled_reason_when_already_chatted() throws Exception {
        MemoSummary memoSummary = memoSummary(10L, 100L);
        memoSummary.disableChat();
        given(memoService.defaultToday(SERVICE_DATE)).willReturn(SERVICE_DATE);
        given(memoService.listMemoSummaries(USER_ID, SERVICE_DATE)).willReturn(List.of(memoSummary));

        mockMvc.perform(get("/api/memo-summaries")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .param("date", "2026-06-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memoSummaries[0].chatAvailable").value(false))
                .andExpect(jsonPath("$.memoSummaries[0].chatDisabledReason").value("ALREADY_CHATTED"));
    }

    @Test
    @DisplayName("MVP 계약에 없는 Memo 수정 endpoint는 노출하지 않는다")
    void update_memo_endpoint_is_not_exposed_in_mvp_contract() throws Exception {
        mockMvc.perform(patch("/api/memos/1")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"content":"수정 시도"}
                                """))
                .andExpect(status().is(not(200)));
    }

    private Memo memo(Long id, String content) {
        Memo memo = Memo.draft(USER_ID, SERVICE_DATE, content, CREATED_AT);
        ReflectionTestUtils.setField(memo, "id", id);
        return memo;
    }

    private MemoSummary memoSummary(Long id, Long itemId) {
        MemoSummaryItem item = new MemoSummaryItem(1L, "오늘 배운 내용");
        ReflectionTestUtils.setField(item, "id", itemId);
        MemoSummary memoSummary = MemoSummary.create(
                USER_ID,
                SERVICE_DATE,
                "학습",
                "오늘 배운 내용",
                List.of(item),
                CREATED_AT
        );
        ReflectionTestUtils.setField(memoSummary, "id", id);
        return memoSummary;
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

package com.girinlog.retrospective.controller;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.error.GlobalExceptionHandler;
import com.girinlog.retrospective.RetrospectiveErrorCode;
import com.girinlog.retrospective.domain.Retrospective;
import com.girinlog.retrospective.service.RetrospectivePage;
import com.girinlog.retrospective.service.RetrospectiveService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RetrospectiveController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, RetrospectiveControllerTest.JwtArgumentResolverConfig.class})
class RetrospectiveControllerTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 6, 8);
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RetrospectiveService retrospectiveService;

    @Test
    @DisplayName("Retrospective를 생성하면 201과 생성된 Markdown을 반환한다")
    void create_retrospective_returns_created_retrospective() throws Exception {
        given(retrospectiveService.createRetrospective(USER_ID, START_DATE, END_DATE))
                .willReturn(retrospective(10L));

        mockMvc.perform(post("/api/retrospectives")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"startDate":"2026-06-01","endDate":"2026-06-08"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.startDate").value("2026-06-01"))
                .andExpect(jsonPath("$.endDate").value("2026-06-08"))
                .andExpect(jsonPath("$.sourceDailyChatSessionIds[0]").value(100))
                .andExpect(jsonPath("$.title").value("첫 주 회고"))
                .andExpect(jsonPath("$.markdown").value("# 첫 주 회고"))
                .andExpect(jsonPath("$.createdAt").value("2026-06-09T09:00:00+09:00"));
    }

    @Test
    @DisplayName("startDate가 없으면 400 에러 envelope를 반환한다")
    void create_retrospective_requires_start_date() throws Exception {
        mockMvc.perform(post("/api/retrospectives")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"endDate":"2026-06-08"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.startDate").exists());
    }

    @Test
    @DisplayName("기간 내 대화가 없으면 422와 NO_RETROSPECTIVE_SOURCE_SESSION을 반환한다")
    void create_retrospective_without_source_session_returns_422() throws Exception {
        given(retrospectiveService.createRetrospective(USER_ID, START_DATE, END_DATE))
                .willThrow(new BusinessException(RetrospectiveErrorCode.NO_RETROSPECTIVE_SOURCE_SESSION));

        mockMvc.perform(post("/api/retrospectives")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .contentType("application/json")
                        .content("""
                                {"startDate":"2026-06-01","endDate":"2026-06-08"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_RETROSPECTIVE_SOURCE_SESSION"))
                .andExpect(jsonPath("$.error.details").value(nullValue()));
    }

    @Test
    @DisplayName("Retrospective 상세 조회는 저장된 Markdown을 반환한다")
    void get_retrospective_returns_retrospective() throws Exception {
        given(retrospectiveService.getRetrospective(USER_ID, 10L)).willReturn(retrospective(10L));

        mockMvc.perform(get("/api/retrospectives/10")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.markdown").value("# 첫 주 회고"));
    }

    @Test
    @DisplayName("Retrospective 목록은 items와 nextCursor를 반환한다")
    void list_retrospectives_returns_items_and_next_cursor() throws Exception {
        given(retrospectiveService.listRetrospectives(eq(USER_ID), isNull(), eq(1)))
                .willReturn(new RetrospectivePage(List.of(retrospective(10L)), "next"));

        mockMvc.perform(get("/api/retrospectives")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.nextCursor").value("next"));
    }

    private Retrospective retrospective(Long id) {
        Retrospective retrospective = Retrospective.create(
                USER_ID,
                START_DATE,
                END_DATE,
                List.of(100L),
                "첫 주 회고",
                "# 첫 주 회고",
                CREATED_AT
        );
        ReflectionTestUtils.setField(retrospective, "id", id);
        return retrospective;
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

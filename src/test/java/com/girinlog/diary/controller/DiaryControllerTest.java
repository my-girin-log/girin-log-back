package com.girinlog.diary.controller;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.error.GlobalExceptionHandler;
import com.girinlog.diary.DiaryErrorCode;
import com.girinlog.diary.domain.Diary;
import com.girinlog.diary.service.DiaryPage;
import com.girinlog.diary.service.DiaryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DiaryController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, DiaryControllerTest.JwtArgumentResolverConfig.class})
class DiaryControllerTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 8);
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-09T06:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiaryService diaryService;

    @Test
    @DisplayName("Diary 상세 조회는 OpenAPI Diary 스키마를 반환한다")
    void get_diary_by_date_returns_diary() throws Exception {
        given(diaryService.getDiaryByDate(USER_ID, SERVICE_DATE)).willReturn(diary(10L, SERVICE_DATE));

        mockMvc.perform(get("/api/diaries/2026-06-08")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.date").value("2026-06-08"))
                .andExpect(jsonPath("$.summary").value("하루 요약"))
                .andExpect(jsonPath("$.mainEvents[0]").value("페어 프로그래밍"))
                .andExpect(jsonPath("$.emotionContext").value("막막했지만 정리됐다."))
                .andExpect(jsonPath("$.concerns").value("테스트 경계"))
                .andExpect(jsonPath("$.newCriteria").value("작게 검증하기"))
                .andExpect(jsonPath("$.nextActions").value("다음엔 먼저 테스트를 쓴다."))
                .andExpect(jsonPath("$.markdown").value("# Diary"))
                .andExpect(jsonPath("$.createdAt").value("2026-06-09T06:00:00+09:00"));
    }

    @Test
    @DisplayName("Diary가 아직 없으면 404와 DIARY_NOT_FOUND를 반환한다")
    void get_diary_by_date_returns_404_when_not_found() throws Exception {
        given(diaryService.getDiaryByDate(USER_ID, SERVICE_DATE))
                .willThrow(new BusinessException(DiaryErrorCode.DIARY_NOT_FOUND));

        mockMvc.perform(get("/api/diaries/2026-06-08")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DIARY_NOT_FOUND"))
                .andExpect(jsonPath("$.error.details").value(nullValue()));
    }

    @Test
    @DisplayName("Diary 목록은 items와 nextCursor를 반환한다")
    void list_diaries_returns_items_and_next_cursor() throws Exception {
        given(diaryService.listDiaries(eq(USER_ID), eq(SERVICE_DATE), eq(LocalDate.of(2026, 6, 9)), isNull(), eq(1)))
                .willReturn(new DiaryPage(List.of(diary(10L, SERVICE_DATE)), "next"));

        mockMvc.perform(get("/api/diaries")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .param("startDate", "2026-06-08")
                        .param("endDate", "2026-06-09")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.nextCursor").value("next"));
    }

    @Test
    @DisplayName("Diary 달력 조회는 Diary가 존재하는 날짜만 반환한다")
    void list_diary_dates_returns_dates() throws Exception {
        given(diaryService.listDiaryDates(USER_ID, SERVICE_DATE, LocalDate.of(2026, 6, 9)))
                .willReturn(List.of(SERVICE_DATE, LocalDate.of(2026, 6, 9)));

        mockMvc.perform(get("/api/diaries/calendar")
                        .with(jwt().jwt(jwt -> jwt.subject(String.valueOf(USER_ID))))
                        .param("startDate", "2026-06-08")
                        .param("endDate", "2026-06-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates[0]").value("2026-06-08"))
                .andExpect(jsonPath("$.dates[1]").value("2026-06-09"));
    }

    private Diary diary(Long id, LocalDate serviceDate) {
        Diary diary = Diary.create(
                USER_ID,
                serviceDate,
                "하루 요약",
                List.of("페어 프로그래밍"),
                "막막했지만 정리됐다.",
                "테스트 경계",
                "작게 검증하기",
                "다음엔 먼저 테스트를 쓴다.",
                "# Diary",
                CREATED_AT
        );
        ReflectionTestUtils.setField(diary, "id", id);
        return diary;
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

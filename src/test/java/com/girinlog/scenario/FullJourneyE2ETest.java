package com.girinlog.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.auth.domain.User;
import com.girinlog.auth.jwt.JwtProvider;
import com.girinlog.auth.repository.UserRepository;
import com.girinlog.batch.DailyResetBatchService;
import com.girinlog.common.time.ServiceDay;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.conversation.service.DailyChatQuestionGenerator;
import com.girinlog.diary.service.DiaryContent;
import com.girinlog.diary.service.DiaryGenerator;
import com.girinlog.memo.domain.MemoSummary;
import com.girinlog.memo.service.MemoSummaryCandidate;
import com.girinlog.memo.service.MemoSummaryGenerator;
import com.girinlog.memo.service.MemoSummaryItemCandidate;
import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.generation.PersonaGenerator;
import com.girinlog.retrospective.service.GeneratedRetrospective;
import com.girinlog.retrospective.service.RetrospectiveGenerator;
import com.girinlog.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전체 사용자 시나리오 E2E: 로그인 토큰 → 온보딩 → 메모 → 요약 → 대화 → 종료 → 06:00 배치 → Diary → 회고.
 * 실제 HTTP(MockMvc) + Testcontainers PostgreSQL. LLM 생성기는 결정적 스텁으로 대체(외부 의존 제거).
 */
@Import(FullJourneyE2ETest.StubGenerators.class)
class FullJourneyE2ETest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private DailyResetBatchService dailyResetBatchService;
    @Autowired
    private Clock clock;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("온보딩→메모→요약→대화→종료→06:00 배치→Diary→회고가 한 흐름으로 동작한다")
    void fullJourney() throws Exception {
        User user = userRepository.save(User.fromGithub("gh-e2e", "octocat", null));
        String auth = "Bearer " + jwtProvider.issueAccessToken(user.id());
        LocalDate today = ServiceDay.today(clock);

        // 1) 온보딩 → Persona 생성
        perform(post("/api/onboarding/submissions"), auth,
                "{\"answers\":[{\"questionId\":1,\"answer\":\"순서대로 정리해요\"}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
        mockMvc.perform(get("/api/personas/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").exists());

        // 2) 메모 작성
        perform(post("/api/memos"), auth, "{\"content\":\"오늘 배운 것\"}")
                .andExpect(status().isCreated());

        // 3) 요약 → MemoSummary
        JsonNode summaryResult = body(perform(post("/api/memos/summaries"), auth, "{}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memoSummaries[0].chatAvailable").value(true)));
        long memoSummaryId = summaryResult.get("memoSummaries").get(0).get("id").asLong();

        // 4) 대화 시작
        JsonNode session = body(perform(post("/api/daily-chat-sessions"), auth,
                "{\"memoSummaryIds\":[" + memoSummaryId + "]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN")));
        long sessionId = session.get("sessionId").asLong();

        // 5) 이미 사용된 MemoSummary로 재시작 → 422
        perform(post("/api/daily-chat-sessions"), auth, "{\"memoSummaryIds\":[" + memoSummaryId + "]}")
                .andExpect(status().isUnprocessableEntity());

        // 6) 답변 → 역질문 이어짐(OPEN)
        perform(post("/api/daily-chat-sessions/" + sessionId + "/answers"), auth, "{\"content\":\"답변이에요\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        // 7) 끝내기 → ENDED(USER_ENDED)
        perform(post("/api/daily-chat-sessions/" + sessionId + "/end"), auth, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.endedReason").value("USER_ENDED"));

        // 8) 06:00 배치(전날 작업공간 정리) — 같은 serviceDate로 실행
        dailyResetBatchService.runDailyReset(today);

        // 9) Diary 생성 확인
        mockMvc.perform(get("/api/diaries/" + today).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markdown").value("# Diary"));
        mockMvc.perform(get("/api/diaries/calendar")
                        .header("Authorization", auth)
                        .param("startDate", today.toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates[0]").value(today.toString()));

        // 10) 회고 생성/조회
        JsonNode retrospective = body(perform(post("/api/retrospectives"), auth,
                "{\"startDate\":\"" + today + "\",\"endDate\":\"" + today + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("회고 제목")));
        long retrospectiveId = retrospective.get("id").asLong();
        mockMvc.perform(get("/api/retrospectives/" + retrospectiveId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markdown").value("# 회고"));

        // 11) 커서/날짜 파라미터 없는 목록 조회(null 파라미터 PostgreSQL 회귀 방지)
        mockMvc.perform(get("/api/diaries").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].markdown").value("# Diary"));
        mockMvc.perform(get("/api/retrospectives").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("회고 제목"));
    }

    @Test
    @DisplayName("기간 내 대화 세션이 없으면 회고 생성은 422")
    void retrospectiveWithoutSessionsReturns422() throws Exception {
        User user = userRepository.save(User.fromGithub("gh-e2e-2", "hubot", null));
        String auth = "Bearer " + jwtProvider.issueAccessToken(user.id());
        LocalDate today = ServiceDay.today(clock);

        perform(post("/api/retrospectives"), auth,
                "{\"startDate\":\"" + today + "\",\"endDate\":\"" + today + "\"}")
                .andExpect(status().isUnprocessableEntity());
    }

    private org.springframework.test.web.servlet.ResultActions perform(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            String auth,
            String jsonBody) throws Exception {
        builder.header("Authorization", auth);
        if (jsonBody != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return mockMvc.perform(builder);
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        MvcResult result = actions.andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @TestConfiguration
    static class StubGenerators {

        @Bean
        @Primary
        MemoSummaryGenerator memoSummaryGenerator() {
            return memos -> List.of(new MemoSummaryCandidate("기록", "요약",
                    memos.stream().map(memo -> new MemoSummaryItemCandidate(memo.id(), memo.content())).toList()));
        }

        @Bean
        @Primary
        DailyChatQuestionGenerator dailyChatQuestionGenerator() {
            return new DailyChatQuestionGenerator() {
                @Override
                public String generateFirstQuestion(List<MemoSummary> memoSummaries) {
                    return "첫 질문";
                }

                @Override
                public String generateFollowUpQuestion(DailyChatSession session) {
                    return "다음 질문";
                }

                @Override
                public boolean shouldEnd(DailyChatSession session) {
                    return false;
                }

                @Override
                public String generateClosingMessage(DailyChatSession session, EndedReason endedReason) {
                    return "마무리";
                }
            };
        }

        @Bean
        @Primary
        DiaryGenerator diaryGenerator() {
            return (serviceDate, sessions) -> new DiaryContent(
                    "하루 요약", List.of("사건"), "감정", "고민", "기준", "다음", "# Diary");
        }

        @Bean
        @Primary
        RetrospectiveGenerator retrospectiveGenerator() {
            return (startDate, endDate, sessions, personaMarkdown) -> new GeneratedRetrospective("회고 제목", "# 회고");
        }

        @Bean
        @Primary
        PersonaGenerator personaGenerator() {
            return input -> new GeneratedPersona(
                    "담백한", "사고 흐름", List.of("회고"), "정리 습관", "회고 기준", "선호 구조", "요약", "# persona");
        }
    }
}

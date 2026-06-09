package com.girinlog.persona;

import com.girinlog.auth.domain.User;
import com.girinlog.auth.jwt.JwtProvider;
import com.girinlog.auth.repository.UserRepository;
import com.girinlog.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PersonaIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtProvider jwtProvider;

    private String tokenFor(String githubId) {
        User user = userRepository.save(User.fromGithub(githubId, "octocat", null));
        return jwtProvider.issueAccessToken(user.id());
    }

    @Test
    @DisplayName("온보딩 제출 → 201 + Persona 생성, 이후 내 Persona 조회 200")
    void submitOnboardingThenGetPersona() throws Exception {
        String token = tokenFor("gh-onb-1");

        mockMvc.perform(post("/api/onboarding/submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"answer\":\"순서대로 정리해요\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personaId").isNumber())
                .andExpect(jsonPath("$.onboardingCompleted").value(true));

        mockMvc.perform(get("/api/personas/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").exists())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    @DisplayName("온보딩 전 Persona 조회는 404")
    void getPersonaBeforeOnboardingReturns404() throws Exception {
        String token = tokenFor("gh-onb-2");

        mockMvc.perform(get("/api/personas/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERSONA_NOT_FOUND"));
    }

    @Test
    @DisplayName("설문 없이 제출하면 400")
    void submitWithoutAnswersReturns400() throws Exception {
        String token = tokenFor("gh-onb-3");

        mockMvc.perform(post("/api/onboarding/submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isBadRequest());
    }
}

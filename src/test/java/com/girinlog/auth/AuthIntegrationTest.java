package com.girinlog.auth;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuthIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("토큰 없이 /api/users/me 호출 시 401")
    void getMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 Bearer 토큰으로 내 정보를 조회한다")
    void getMeWithTokenReturnsUser() throws Exception {
        User user = userRepository.save(User.fromGithub("gh-1", "octocat", "https://img/o.png"));
        String token = jwtProvider.issueAccessToken(user.id());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubUsername").value("octocat"))
                .andExpect(jsonPath("$.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("닉네임을 수정한다")
    void updateNickname() throws Exception {
        User user = userRepository.save(User.fromGithub("gh-2", "hubot", null));
        String token = jwtProvider.issueAccessToken(user.id());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"기린\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("기린"));
    }

    @Test
    @DisplayName("닉네임 형식 위반(빈 값)은 400")
    void updateNicknameBlankReturns400() throws Exception {
        User user = userRepository.save(User.fromGithub("gh-3", "mona", null));
        String token = jwtProvider.issueAccessToken(user.id());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}

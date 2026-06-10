package com.girinlog.auth.controller;

import com.girinlog.auth.oauth.GithubOAuthProperties;
import com.girinlog.auth.service.AuthService;
import com.girinlog.common.error.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 웹 계층 테스트(standalone). 보안 필터 없이 state CSRF·콜백·리다이렉트 로직만 검증.
 */
class AuthControllerTest {

    private static final String STATE_COOKIE = "oauth_state";

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        GithubOAuthProperties properties = new GithubOAuthProperties();
        properties.setFrontendRedirectUri("https://front.example.com/oauth/callback");
        AuthController controller = new AuthController(authService, properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/auth/github → 302 + state HttpOnly 쿠키 + GitHub로 리다이렉트")
    void redirectSetsStateCookie() throws Exception {
        when(authService.buildAuthorizeUrl(anyString()))
                .thenReturn("https://github.com/login/oauth/authorize?state=x");

        mockMvc.perform(get("/api/auth/github"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("github.com/login/oauth/authorize")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(STATE_COOKIE + "=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    @DisplayName("callback: state(쿠키=파라미터) 일치 → 로그인 후 토큰을 프론트로 302")
    void callbackWithMatchingStateRedirectsWithToken() throws Exception {
        when(authService.loginByGithubCode("code-1")).thenReturn("jwt-token");

        mockMvc.perform(get("/api/auth/github/callback")
                        .param("code", "code-1")
                        .param("state", "s1")
                        .cookie(new Cookie(STATE_COOKIE, "s1")))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "https://front.example.com/oauth/callback#token=jwt-token"));
    }

    @Test
    @DisplayName("callback: state 불일치 → 401 OAUTH_STATE_MISMATCH")
    void callbackWithMismatchedStateReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/github/callback")
                        .param("code", "code-1")
                        .param("state", "s1")
                        .cookie(new Cookie(STATE_COOKIE, "different")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH_STATE_MISMATCH"));
    }

    @Test
    @DisplayName("callback: state 쿠키 없음 → 401 OAUTH_STATE_MISMATCH")
    void callbackWithoutStateCookieReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/github/callback")
                        .param("code", "code-1")
                        .param("state", "s1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH_STATE_MISMATCH"));
    }

    @Test
    @DisplayName("POST /api/auth/logout → 204")
    void logoutReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("redirect와 callback의 state 쿠키 속성(SameSite/Path) 확인")
    void stateCookieAttributes() throws Exception {
        when(authService.buildAuthorizeUrl(anyString())).thenReturn("https://github.com/x");

        mockMvc.perform(get("/api/auth/github"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }
}

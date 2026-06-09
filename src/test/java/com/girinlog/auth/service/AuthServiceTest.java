package com.girinlog.auth.service;

import com.girinlog.auth.domain.User;
import com.girinlog.auth.jwt.JwtProvider;
import com.girinlog.auth.oauth.GithubOAuthClient;
import com.girinlog.auth.oauth.GithubUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GithubOAuthClient githubOAuthClient;

    @Mock
    private UserService userService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("authorize URL 생성은 GitHub 클라이언트에 위임한다")
    void buildAuthorizeUrlDelegates() {
        when(githubOAuthClient.authorizeUrl("state-1")).thenReturn("https://github.com/login/oauth/authorize?state=state-1");

        String url = authService.buildAuthorizeUrl("state-1");

        assertThat(url).contains("state=state-1");
    }

    @Test
    @DisplayName("code → GitHub 사용자 → 로그인 → 자체 토큰 발급 흐름")
    void loginByGithubCodeIssuesToken() {
        GithubUser githubUser = new GithubUser("gh-1", "octocat", "https://img/o.png");
        User user = User.fromGithub("gh-1", "octocat", "https://img/o.png");
        when(githubOAuthClient.exchangeCodeForUser("code-1")).thenReturn(githubUser);
        when(userService.loginWithGithub("gh-1", "octocat", "https://img/o.png")).thenReturn(user);
        when(jwtProvider.issueAccessToken(any())).thenReturn("issued-token");

        String token = authService.loginByGithubCode("code-1");

        assertThat(token).isEqualTo("issued-token");
        verify(userService).loginWithGithub("gh-1", "octocat", "https://img/o.png");
        verify(jwtProvider).issueAccessToken(any());
    }
}

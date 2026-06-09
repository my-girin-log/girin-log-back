package com.girinlog.auth.service;

import com.girinlog.auth.domain.User;
import com.girinlog.auth.jwt.JwtProvider;
import com.girinlog.auth.oauth.GithubOAuthClient;
import com.girinlog.auth.oauth.GithubUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GitHub OAuth 로그인 흐름 조립: 인증 URL 생성 → code 교환 → User 로그인 → 자체 JWT 발급.
 */
@Service
public class AuthService {

    private final GithubOAuthClient githubOAuthClient;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    public AuthService(GithubOAuthClient githubOAuthClient, UserService userService, JwtProvider jwtProvider) {
        this.githubOAuthClient = githubOAuthClient;
        this.userService = userService;
        this.jwtProvider = jwtProvider;
    }

    public String buildAuthorizeUrl(String state) {
        return githubOAuthClient.authorizeUrl(state);
    }

    /** callback의 code로 로그인 처리 후 Access 토큰을 반환한다. */
    @Transactional
    public String loginByGithubCode(String code) {
        GithubUser githubUser = githubOAuthClient.exchangeCodeForUser(code);
        User user = userService.loginWithGithub(
                githubUser.githubId(), githubUser.username(), githubUser.profileImageUrl());
        return jwtProvider.issueAccessToken(user.id());
    }
}

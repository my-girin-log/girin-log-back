package com.girinlog.auth.controller;

import com.girinlog.auth.oauth.GithubOAuthProperties;
import com.girinlog.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * GitHub OAuth 로그인 엔드포인트. (openapi /api/auth/*)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GithubOAuthProperties githubOAuthProperties;

    public AuthController(AuthService authService, GithubOAuthProperties githubOAuthProperties) {
        this.authService = authService;
        this.githubOAuthProperties = githubOAuthProperties;
    }

    /** GitHub 인증 페이지로 302 리다이렉트. */
    @GetMapping("/github")
    public ResponseEntity<Void> redirectToGitHub() {
        // TODO(BE-A): state를 세션/쿠키에 저장해 callback에서 CSRF 검증. MVP는 생성만.
        String state = UUID.randomUUID().toString();
        URI authorizeUri = URI.create(authService.buildAuthorizeUrl(state));
        return ResponseEntity.status(HttpStatus.FOUND).location(authorizeUri).build();
    }

    /** code를 받아 로그인 처리 후 토큰을 담아 프론트로 302 리다이렉트. */
    @GetMapping("/github/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        String accessToken = authService.loginByGithubCode(code);
        URI frontend = URI.create(
                githubOAuthProperties.getFrontendRedirectUri() + "#token=" + accessToken);
        return ResponseEntity.status(HttpStatus.FOUND).location(frontend).build();
    }

    /** stateless 토큰 방식이므로 서버는 별도 무효화 없이 204. 토큰 폐기는 클라이언트가 한다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}

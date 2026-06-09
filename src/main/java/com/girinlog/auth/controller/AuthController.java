package com.girinlog.auth.controller;

import com.girinlog.auth.exception.AuthErrorCode;
import com.girinlog.auth.oauth.GithubOAuthProperties;
import com.girinlog.auth.service.AuthService;
import com.girinlog.common.error.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * GitHub OAuth 로그인 엔드포인트. (openapi /api/auth/*)
 *
 * <p>CSRF 방지를 위해 인증 시작 시 state를 단기 HttpOnly 쿠키에 저장하고 callback에서 대조한다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String STATE_COOKIE = "oauth_state";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final AuthService authService;
    private final GithubOAuthProperties githubOAuthProperties;

    public AuthController(AuthService authService, GithubOAuthProperties githubOAuthProperties) {
        this.authService = authService;
        this.githubOAuthProperties = githubOAuthProperties;
    }

    /** GitHub 인증 페이지로 302 리다이렉트하고, CSRF state를 쿠키에 심는다. */
    @GetMapping("/github")
    public ResponseEntity<Void> redirectToGitHub() {
        String state = UUID.randomUUID().toString();
        URI authorizeUri = URI.create(authService.buildAuthorizeUrl(state));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authorizeUri)
                .header(HttpHeaders.SET_COOKIE, stateCookie(state, STATE_TTL).toString())
                .build();
    }

    /** code를 받아 state 대조 후 로그인 처리하고, 토큰을 담아 프론트로 302 리다이렉트한다. */
    @GetMapping("/github/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @CookieValue(name = STATE_COOKIE, required = false) String savedState) {
        verifyState(state, savedState);
        String accessToken = authService.loginByGithubCode(code);
        URI frontend = URI.create(
                githubOAuthProperties.getFrontendRedirectUri() + "#token=" + accessToken);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(frontend)
                .header(HttpHeaders.SET_COOKIE, stateCookie("", Duration.ZERO).toString())
                .build();
    }

    /** stateless 토큰 방식이므로 서버는 별도 무효화 없이 204. 토큰 폐기는 클라이언트가 한다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private void verifyState(String state, String savedState) {
        if (savedState == null || state == null || !savedState.equals(state)) {
            throw new BusinessException(AuthErrorCode.OAUTH_STATE_MISMATCH);
        }
    }

    private ResponseCookie stateCookie(String value, Duration maxAge) {
        // TODO(BE-A): 운영(HTTPS)에서는 secure(true) 적용. 로컬 http 호환 위해 현재 false.
        return ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .secure(false)
                .maxAge(maxAge)
                .build();
    }
}

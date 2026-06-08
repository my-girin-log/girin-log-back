package com.girinlog.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 보안 설정 골격. (conventions/api.md 3절: GitHub OAuth 로그인 → 자체 JWT, stateless)
 *
 * <p>현재는 부팅 가능한 최소 형태로 모든 요청을 허용한다. 인증/인가 정책 변경은
 * <b>사용자 승인 필요</b>(AGENTS.md "AI 금지 작업") — JWT 발급/검증·엔드포인트 보호는
 * api/openapi.yaml 합의 후 BE-A 가 채운다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO(BE-A): openapi 합의 후 인증 규칙으로 교체. 지금은 부팅용 permitAll.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

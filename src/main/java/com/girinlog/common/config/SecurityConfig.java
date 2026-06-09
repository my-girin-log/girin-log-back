package com.girinlog.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 앱 전역 보안 체인. (conventions/api.md 3절: GitHub OAuth → 자체 JWT, stateless Bearer)
 *
 * <p>자체 JWT 검증은 OAuth2 Resource Server로 처리한다. {@code JwtDecoder} 빈은 auth 모듈
 * ({@code auth.jwt.JwtConfig})이 HMAC 시크릿으로 제공한다.
 * 인증/인가 정책 변경은 사용자 승인 사항이다(AGENTS.md).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }
}

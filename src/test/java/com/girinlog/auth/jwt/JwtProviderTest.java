package com.girinlog.auth.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "girinlog-test-secret-girinlog-test-secret"; // >= 32 bytes (HS256)
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneOffset.UTC);
    private final JwtProvider jwtProvider = new JwtProvider(SECRET, Duration.ofDays(14), clock);

    private JwtDecoder decoder() {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    @DisplayName("subject=userId 로 토큰을 발급하고 같은 시크릿으로 검증된다")
    void issuesTokenWithUserIdSubject() {
        String token = jwtProvider.issueAccessToken(42L);

        Jwt decoded = decoder().decode(token);

        assertThat(decoded.getSubject()).isEqualTo("42");
    }

    @Test
    @DisplayName("만료는 발급 시각 + TTL(14일)이다")
    void expiryIsIssuedAtPlusTtl() {
        String token = jwtProvider.issueAccessToken(1L);

        Jwt decoded = decoder().decode(token);

        assertThat(decoded.getIssuedAt()).isEqualTo(clock.instant());
        assertThat(decoded.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofDays(14)));
    }
}

package com.girinlog.auth.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 자체 Access 토큰(HMAC-SHA256) 발급기. 검증은 SecurityConfig의 {@code JwtDecoder}가 담당한다.
 * 시각은 주입된 {@link Clock}을 통해 결정해 테스트 가능하게 한다(coding.md 7절).
 */
public class JwtProvider {

    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtProvider(String secret, Duration accessTokenTtl, Clock clock) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        this.accessTokenTtl = accessTokenTtl;
        this.clock = clock;
    }

    /** subject = userId 로 Access 토큰을 발급한다. */
    public String issueAccessToken(Long userId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}

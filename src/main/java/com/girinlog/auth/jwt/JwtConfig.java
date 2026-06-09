package com.girinlog.auth.jwt;

import com.girinlog.auth.oauth.GithubOAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * auth 모듈이 소유하는 JWT/OAuth 설정. 발급기({@link JwtProvider})와 검증기({@link JwtDecoder})를
 * 같은 HMAC 시크릿으로 구성한다.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, GithubOAuthProperties.class})
public class JwtConfig {

    @Bean
    public JwtProvider jwtProvider(JwtProperties properties, Clock serviceClock) {
        return new JwtProvider(properties.getSecret(), properties.getAccessTokenTtl(), serviceClock);
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        SecretKeySpec key = new SecretKeySpec(
                properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}

package com.girinlog.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 자체 JWT 설정. (auth, HMAC-SHA256 · Bearer · Access 단독)
 * 시크릿은 환경변수로만 주입한다(HS256은 32바이트 이상 필요).
 */
@ConfigurationProperties("girinlog.jwt")
public class JwtProperties {

    private String secret;
    private Duration accessTokenTtl = Duration.ofDays(14);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }
}

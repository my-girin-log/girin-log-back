package com.girinlog.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub OAuth(수동 흐름) 설정. 자격증명은 환경변수로만 주입한다.
 */
@ConfigurationProperties("girinlog.oauth.github")
public class GithubOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String frontendRedirectUri;
    private String scope = "read:user";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getFrontendRedirectUri() {
        return frontendRedirectUri;
    }

    public void setFrontendRedirectUri(String frontendRedirectUri) {
        this.frontendRedirectUri = frontendRedirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}

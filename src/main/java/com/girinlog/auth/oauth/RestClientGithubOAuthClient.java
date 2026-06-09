package com.girinlog.auth.oauth;

import com.girinlog.auth.exception.AuthErrorCode;
import com.girinlog.common.error.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * RestClient 기반 GitHub OAuth 어댑터. 프롬프트/외부 호출은 이 어댑터 뒤로 감춘다(coding.md 8절 취지).
 */
@Component
public class RestClientGithubOAuthClient implements GithubOAuthClient {

    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";

    private final GithubOAuthProperties properties;
    private final RestClient restClient;

    public RestClientGithubOAuthClient(GithubOAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public String authorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("scope", properties.getScope())
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public GithubUser exchangeCodeForUser(String code) {
        String accessToken = requestAccessToken(code);
        GithubUserResponse user = requestGithubUser(accessToken);
        if (user == null || user.id() == null) {
            throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
        }
        return new GithubUser(String.valueOf(user.id()), user.login(), user.avatarUrl());
    }

    private String requestAccessToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());

        AccessTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AccessTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
        }
        return response.accessToken();
    }

    private GithubUserResponse requestGithubUser(String accessToken) {
        return restClient.get()
                .uri(USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GithubUserResponse.class);
    }

    private record AccessTokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {
    }

    private record GithubUserResponse(
            Long id,
            String login,
            @com.fasterxml.jackson.annotation.JsonProperty("avatar_url") String avatarUrl) {
    }
}

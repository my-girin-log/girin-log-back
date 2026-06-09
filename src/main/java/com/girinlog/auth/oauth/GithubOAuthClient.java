package com.girinlog.auth.oauth;

/**
 * GitHub OAuth 연동 포트. 도메인/서비스는 이 인터페이스에만 의존한다(외부 연동 격리).
 */
public interface GithubOAuthClient {

    /** state를 포함한 GitHub 인증 페이지 URL. */
    String authorizeUrl(String state);

    /** authorization code를 access token으로 교환하고 GitHub 사용자 정보를 가져온다. */
    GithubUser exchangeCodeForUser(String code);
}

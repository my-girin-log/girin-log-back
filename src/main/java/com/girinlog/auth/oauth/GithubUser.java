package com.girinlog.auth.oauth;

/**
 * GitHub에서 받아온 최소 사용자 식별 정보.
 */
public record GithubUser(String githubId, String username, String profileImageUrl) {
}

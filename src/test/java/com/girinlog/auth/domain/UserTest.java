package com.girinlog.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("GitHub 정보로 생성하면 온보딩 미완료·닉네임 없음 상태다")
    void fromGithubStartsNotOnboarded() {
        User user = User.fromGithub("gh-1", "octocat", "https://img/o.png");

        assertThat(user.githubId()).isEqualTo("gh-1");
        assertThat(user.githubUsername()).isEqualTo("octocat");
        assertThat(user.profileImageUrl()).isEqualTo("https://img/o.png");
        assertThat(user.nickname()).isNull();
        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("재로그인 시 GitHub 프로필을 최신화한다")
    void syncGithubProfileUpdatesUsernameAndImage() {
        User user = User.fromGithub("gh-1", "octocat", "https://img/old.png");

        user.syncGithubProfile("octocat-new", "https://img/new.png");

        assertThat(user.githubUsername()).isEqualTo("octocat-new");
        assertThat(user.profileImageUrl()).isEqualTo("https://img/new.png");
        assertThat(user.githubId()).isEqualTo("gh-1");
    }

    @Test
    @DisplayName("닉네임을 변경할 수 있다")
    void changeNickname() {
        User user = User.fromGithub("gh-1", "octocat", null);

        user.changeNickname("기린");

        assertThat(user.nickname()).isEqualTo("기린");
    }

    @Test
    @DisplayName("온보딩을 완료 처리할 수 있다")
    void completeOnboarding() {
        User user = User.fromGithub("gh-1", "octocat", null);

        user.completeOnboarding();

        assertThat(user.isOnboardingCompleted()).isTrue();
    }
}

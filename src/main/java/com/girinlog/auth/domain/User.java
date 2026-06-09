package com.girinlog.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * GitHub OAuth 기반 로그인 사용자. (domain/data-model.md User)
 *
 * <p>상태 변경은 의도가 드러나는 메서드로만 한다(setter를 열지 않는다, coding.md 5-5).
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_github_id", columnNames = "github_id"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false, unique = true)
    private String githubId;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected User() {
    }

    private User(String githubId, String githubUsername, String profileImageUrl) {
        this.githubId = githubId;
        this.githubUsername = githubUsername;
        this.profileImageUrl = profileImageUrl;
        this.onboardingCompleted = false;
    }

    /** 최초 로그인 시 GitHub 최소 식별 정보로 생성한다(기능요구 1-1). */
    public static User fromGithub(String githubId, String githubUsername, String profileImageUrl) {
        return new User(githubId, githubUsername, profileImageUrl);
    }

    /** 재로그인 시 GitHub 프로필을 최신화한다. */
    public void syncGithubProfile(String githubUsername, String profileImageUrl) {
        this.githubUsername = githubUsername;
        this.profileImageUrl = profileImageUrl;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long id() {
        return id;
    }

    public String githubId() {
        return githubId;
    }

    public String githubUsername() {
        return githubUsername;
    }

    public String profileImageUrl() {
        return profileImageUrl;
    }

    public String nickname() {
        return nickname;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}

package com.girinlog.auth.service;

import com.girinlog.auth.domain.User;
import com.girinlog.auth.exception.AuthErrorCode;
import com.girinlog.auth.repository.UserRepository;
import com.girinlog.common.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("처음 보는 GitHub 사용자는 새로 생성한다")
    void loginWithGithubCreatesNewUser() {
        when(userRepository.findByGithubId("gh-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.loginWithGithub("gh-1", "octocat", "https://img/o.png");

        assertThat(user.githubId()).isEqualTo("gh-1");
        assertThat(user.isOnboardingCompleted()).isFalse();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("기존 GitHub 사용자는 프로필만 갱신하고 새로 저장하지 않는다")
    void loginWithGithubUpdatesExistingUser() {
        User existing = User.fromGithub("gh-1", "octocat", "https://img/old.png");
        when(userRepository.findByGithubId("gh-1")).thenReturn(Optional.of(existing));

        User user = userService.loginWithGithub("gh-1", "octocat-new", "https://img/new.png");

        assertThat(user).isSameAs(existing);
        assertThat(user.githubUsername()).isEqualTo("octocat-new");
        assertThat(user.profileImageUrl()).isEqualTo("https://img/new.png");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("없는 사용자 조회 시 USER_NOT_FOUND 예외")
    void getByIdThrowsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(thrown ->
                        assertThat(((BusinessException) thrown).errorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("닉네임을 변경한다")
    void changeNickname() {
        User existing = User.fromGithub("gh-1", "octocat", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        User user = userService.changeNickname(1L, "기린");

        assertThat(user.nickname()).isEqualTo("기린");
    }
}

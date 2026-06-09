package com.girinlog.auth.service;

import com.girinlog.auth.domain.User;
import com.girinlog.auth.exception.AuthErrorCode;
import com.girinlog.auth.repository.UserRepository;
import com.girinlog.common.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 조회/수정과 GitHub 로그인 시 User 생성·갱신을 담당한다. (BE-A)
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * GitHub 로그인 처리. 기존 사용자면 프로필을 최신화하고, 없으면 새로 생성한다(기능요구 1-1).
     * 변경은 영속성 컨텍스트의 변경 감지로 반영된다.
     */
    public User loginWithGithub(String githubId, String githubUsername, String profileImageUrl) {
        return userRepository.findByGithubId(githubId)
                .map(existing -> {
                    existing.syncGithubProfile(githubUsername, profileImageUrl);
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.fromGithub(githubId, githubUsername, profileImageUrl)));
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    public User changeNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        user.changeNickname(nickname);
        return user;
    }
}

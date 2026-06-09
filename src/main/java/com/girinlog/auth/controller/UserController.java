package com.girinlog.auth.controller;

import com.girinlog.auth.controller.dto.UpdateUserRequest;
import com.girinlog.auth.controller.dto.UserResponse;
import com.girinlog.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 정보 조회/수정. (openapi /api/users/me)
 * 현재 사용자 id는 자체 JWT의 subject에서 가져온다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.getById(currentUserId(jwt)));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.changeNickname(currentUserId(jwt), request.nickname()));
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
